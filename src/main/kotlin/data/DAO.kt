package data

import java.sql.Connection

interface DAO {

    fun getCompanies(connection: Connection): ArrayList<Company>

    fun getOrders(connection: Connection): ArrayList<Order>

    fun clearCompanies(connection: Connection)

    fun clearOrders(connection: Connection)

    fun clearTables(connection: Connection)

    fun addOrder(connection: Connection, order: Order)

    fun addCompany(connection: Connection, company: Company)

    fun deleteCompanyByTitle(connection: Connection, title: String)

    fun deleteOrderByDescription(connection: Connection, description: String)

    fun deleteOrderById(connection: Connection, id: Int)

    fun deleteCompanyById(connection: Connection, id: Int)

    fun findCompanyByTitle(connection: Connection, title: String): ArrayList<Company>

    fun findOrderByDescription(connection: Connection, description: String): ArrayList<Order>

    fun deleteDatabase(connection: Connection)
}