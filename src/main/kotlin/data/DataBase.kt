package data

import java.sql.*

object DataBase : DAO {

    private const val BUILDER_DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val USER_DATABASE_URL = "jdbc:postgresql://localhost:5432/"

    private var user: DatabaseUser? = null

    fun connect(user: DatabaseUser): Connection? {
        return try {
            val connection = DriverManager.getConnection(BUILDER_DATABASE_URL, user.name, user.password)
            this.user = user
            createDatabase(user, connection)
        } catch (exception: Exception) {
            null
        }
    }

    private fun createDatabase(user: DatabaseUser, connection: Connection): Connection? {
        try {
            val properCase = connection.prepareStatement("SELECT * FROM create_db_if_not_exist (?)")
            properCase.setString(1, user.databaseTitle)
            properCase.execute()
        } catch (e: Exception) {
            println(e.message)
        }
        connection.close()
        val userBaseConnection = DriverManager
            .getConnection("$USER_DATABASE_URL${user.databaseTitle}", user.name, user.password)
        pushFunctions(userBaseConnection)
        createTables(userBaseConnection)
        return userBaseConnection
    }

    private fun pushFunctions(connection: Connection) {
        val sqlFunctions = """
            CREATE OR REPLACE FUNCTION create_tables() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	CREATE TABLE IF NOT EXISTS Companies (
            		id INTEGER PRIMARY KEY,
            		title TEXT,
            		orders_price INTEGER CHECK(orders_price >= 0) DEFAULT 0
            	);
            	CREATE INDEX title_index ON Companies (title);
            	
            	CREATE TABLE IF NOT EXISTS Orders (
            		id INTEGER PRIMARY KEY,
            		company_id INTEGER REFERENCES Companies(id),
            		description TEXT,
            		price INTEGER CHECK(price >= 0)
            	);
            	CREATE INDEX description_index ON Orders (description);
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION delete_tables() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	DROP TABLE IF EXISTS Companies CASCADE;
            	DROP TABLE IF EXISTS Orders CASCADE;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION get_companies() RETURNS
            TABLE(
            	id INTEGER,
            	title TEXT,
            	orders_price INTEGER
            ) AS ${'$'}${'$'}
            BEGIN
            	RETURN QUERY SELECT * FROM Companies;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION get_orders() RETURNS
            TABLE(
            	id INTEGER,
            	company_id INTEGER,
            	description TEXT,
            	price INTEGER
            ) AS ${'$'}${'$'}
            BEGIN
            	RETURN QUERY SELECT * FROM Orders;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION clear_companies() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	TRUNCATE Companies;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION clear_orders() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	TRUNCATE Orders;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION clear_tables() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	TRUNCATE Companies CASCADE;
            	TRUNCATE Orders CASCADE;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION add_order(integer, integer, text, integer) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	INSERT INTO Orders VALUES(${'$'}1, ${'$'}2, ${'$'}3, ${'$'}4);
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION add_company(integer, text) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	INSERT INTO Companies VALUES(${'$'}1, ${'$'}2);
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION delete_orders_by_description(text) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	DELETE FROM orders WHERE description = ${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION delete_companies_by_title(text) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	DELETE FROM Orders WHERE company_id in (SELECT companies.id FROM Companies WHERE title=${'$'}1);
            	DELETE FROM Companies WHERE title=${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION delete_orders_by_id(integer) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	DELETE FROM Orders WHERE id = ${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION delete_companies_by_id(integer) RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	DELETE FROM Orders WHERE company_id = ${'$'}1;
            	DELETE FROM Companies WHERE id = ${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION find_companies_by_title(text) RETURNS
            TABLE(
            	id INTEGER,
            	title TEXT,
            	orders_price INTEGER
            ) AS ${'$'}${'$'}
            BEGIN
            	RETURN QUERY SELECT * FROM Companies WHERE Companies.title=${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION find_orders_by_description(text) RETURNS
            TABLE(
            	id INTEGER,
            	company_id INTEGER,
            	description TEXT,
            	price INTEGER
            ) AS ${'$'}${'$'}
            BEGIN
            	RETURN QUERY SELECT * FROM Orders WHERE Orders.description=${'$'}1;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION calculate_total_price() RETURNS TRIGGER AS ${'$'}${'$'}
            BEGIN
            	UPDATE companies SET orders_price=(SELECT SUM(price) FROM orders WHERE company_id = companies.id);
            	UPDATE companies SET orders_price=0 WHERE orders_price IS null;
            	RETURN NEW;
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';

            CREATE OR REPLACE FUNCTION set_trigger() RETURNS VOID AS ${'$'}${'$'}
            BEGIN
            	CREATE TRIGGER total_price_trigger AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON orders EXECUTE PROCEDURE calculate_total_price();
            END; ${'$'}${'$'}
            LANGUAGE 'plpgsql';
        """
            .trimIndent()
        connection.createStatement().execute(sqlFunctions)
    }

    private fun createTables(connection: Connection) {
        try {
            connection.prepareStatement("SELECT * FROM create_tables ()").execute()
        } catch (e: Exception) {
            println(e.message)
        }
        try {
            connection.prepareStatement("SELECT * FROM set_trigger ()").execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    private fun postgresVoidFunction(connection: Connection, sqlFunction: String) {
        try {
            connection.prepareStatement("SELECT * FROM $sqlFunction").execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun getCompanies(connection: Connection): ArrayList<Company> {
        val companies = ArrayList<Company>()
        try {
            val statement = connection.prepareStatement("SELECT * FROM get_companies()")
            val result = statement.executeQuery()

            while (result.next()) {
                companies.add(
                    Company(
                        result.getInt("id"),
                        result.getString("title"),
                        result.getInt("orders_price")
                    )
                )
            }

        } catch (e: Exception) {
            println(e.message)
        }
        return companies
    }

    override fun getOrders(connection: Connection): ArrayList<Order> {
        val orders = ArrayList<Order>()
        try {
            val statement = connection.prepareStatement("SELECT * FROM get_orders()")
            val result = statement.executeQuery()

            while (result.next()) {
                orders.add(
                    Order(
                        result.getInt("id"),
                        result.getInt("company_id"),
                        result.getString("description"),
                        result.getInt("price")
                    )
                )
            }

        } catch (e: Exception) {
            println(e.message)
        }
        return orders
    }

    override fun clearCompanies(connection: Connection) {
        postgresVoidFunction(connection, "clear_companies()")
    }

    override fun clearOrders(connection: Connection) {
        postgresVoidFunction(connection, "clear_orders()")
    }

    override fun clearTables(connection: Connection) {
        postgresVoidFunction(connection, "clear_tables()")
    }

    override fun addOrder(connection: Connection, order: Order) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM add_order(?, ?, ?, ?)")
            statement.setInt(1, order.id)
            statement.setInt(2, order.companyId)
            statement.setString(3, order.description)
            statement.setInt(4, order.price)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun addCompany(connection: Connection, company: Company) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM add_company(?, ?)")
            statement.setInt(1, company.id)
            statement.setString(2, company.title)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun deleteCompanyByTitle(connection: Connection, title: String) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM delete_companies_by_title(?)")
            statement.setString(1, title)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun deleteOrderByDescription(connection: Connection, description: String) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM delete_orders_by_description(?)")
            statement.setString(1, description)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun deleteOrderById(connection: Connection, id: Int) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM delete_orders_by_id(?)")
            statement.setInt(1, id)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun deleteCompanyById(connection: Connection, id: Int) {
        try {
            val statement =
                connection.prepareStatement("SELECT * FROM delete_companies_by_id(?)")
            statement.setInt(1, id)
            statement.execute()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun findCompanyByTitle(connection: Connection, title: String): ArrayList<Company> {
        val companies = ArrayList<Company>()
        try {
            val statement = connection.prepareStatement("SELECT * FROM find_companies_by_title(?)")
            statement.setString(1, title)
            val result = statement.executeQuery()

            while (result.next()) {
                companies.add(
                    Company(
                        result.getInt("id"),
                        result.getString("title"),
                        result.getInt("orders_price")
                    )
                )
            }

        } catch (e: Exception) {
            println(e.message)
        }
        return companies
    }

    override fun findOrderByDescription(connection: Connection, description: String): ArrayList<Order> {
        val orders = ArrayList<Order>()
        try {
            val statement = connection.prepareStatement("SELECT * FROM find_orders_by_description(?)")
            statement.setString(1, description)
            val result = statement.executeQuery()

            while (result.next()) {
                orders.add(
                    Order(
                        result.getInt("id"),
                        result.getInt("company_id"),
                        result.getString("description"),
                        result.getInt("price")
                    )
                )
            }

        } catch (e: Exception) {
            println(e.message)
        }
        return orders
    }

    override fun deleteDatabase(connection: Connection) {
        user?.let {
            connection.close()
            val connection = DriverManager.getConnection(BUILDER_DATABASE_URL, it.name, it.password)

            try {
                val statement =
                    connection.prepareStatement("SELECT * FROM delete_database(?)")
                statement.setString(1, it.databaseTitle)
                statement.execute()
            } catch (e: Exception) {
                println(e.message)
            }
            connection.close()
            user = null
        }
    }

}