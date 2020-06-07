package presentation

import data.Company
import data.DataBase
import data.DatabaseUser
import data.Order
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.sql.Connection


class MainView : Application() {

    private lateinit var root: Pane
    private lateinit var companyTable: TableView<Company>
    private lateinit var ordersTable: TableView<Order>

    private var isCompaniesShowed = true
    private var isUserLogin = false
    private var actualConnection: Connection? = null

    override fun start(stage: Stage) {
        companyTable = createCompaniesTable()
        ordersTable = createOrdersTable()

        val menuBar = createMenuBar()

        root = VBox()
        root.children.add(menuBar)

        stage.title = "Database"
        val scene = Scene(root, 1000.0, 800.0)
        stage.scene = scene
        stage.show()
    }

    private val companies: ObservableList<Company>
        get() {
            return FXCollections.observableArrayList()
        }

    private val orders: ObservableList<Order>
        get() {
            return FXCollections.observableArrayList()
        }

    private fun createCompaniesTable(): TableView<Company> {
        val idColumn = TableColumn<Company, Long>("Id")
        val titleColumn = TableColumn<Company, String>("Title")
        val totalPriceColumn = TableColumn<Company, Long>("Total price")
        val deleteColumn = TableColumn<Company, Long>("Button Column")

        idColumn.cellValueFactory = PropertyValueFactory("id")
        titleColumn.cellValueFactory = PropertyValueFactory("title")
        totalPriceColumn.cellValueFactory = PropertyValueFactory("totalPrice")

        idColumn.sortType = TableColumn.SortType.DESCENDING

        val table = TableView<Company>()

        table.columns.addAll(idColumn, titleColumn, totalPriceColumn, deleteColumn)
        table.items = companies

        return table
    }

    private fun createOrdersTable(): TableView<Order> {

        val idColumn = TableColumn<Order, Long>("Id")
        val companyIdColumn = TableColumn<Order, Long>("Company id")
        val descriptionColumn = TableColumn<Order, String>("Description")
        val priceColumn = TableColumn<Order, Long>("Price")

        idColumn.cellValueFactory = PropertyValueFactory("id")
        companyIdColumn.cellValueFactory = PropertyValueFactory("companyId")
        descriptionColumn.cellValueFactory = PropertyValueFactory("description")
        priceColumn.cellValueFactory = PropertyValueFactory("price")

        idColumn.sortType = TableColumn.SortType.DESCENDING

        val table = TableView<Order>()
        table.columns.addAll(idColumn, companyIdColumn, descriptionColumn, priceColumn)

        table.items = orders
        return table
    }

    private fun createMenuBar(): MenuBar {
        val menuBar = MenuBar()

        val userMenu = Menu("User")
        val userMenuItemChangeUser = MenuItem("Change user").apply {
            onAction = EventHandler {
                val dialog = Stage().apply {
                    initStyle(StageStyle.UTILITY)
                }
                val box = VBox().apply {
                    alignment = Pos.CENTER
                }
                val buttons = HBox().apply {
                    alignment = Pos.CENTER
                }
                val userName = TextField().apply {
                    promptText = "User"
                }
                val password = TextField().apply {
                    promptText = "Password"
                }
                val databaseName = TextField().apply {
                    promptText = "Database name"
                }
                val btnOK = Button("Ok").apply {
                    onMouseClicked = EventHandler {
                        if (userName.text != "" && password.text != "" && databaseName.text != "") {
                            val user = DatabaseUser(userName.text, password.text, databaseName.text)
                            actualConnection = DataBase.connect(user)
                            if (actualConnection != null) {
                                isUserLogin = true
                                isCompaniesShowed = !isCompaniesShowed
                                switchTables()
                                refreshData()
                                dialog.close()
                            }
                        }
                    }
                }

                buttons.children.addAll(
                    btnOK,
                    Button("Cancel").apply { onMouseClicked = EventHandler { dialog.close() } })

                box.children.addAll(Label("Change user dialog"), userName, password, databaseName, buttons)
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }

        val actionMenu = Menu("Actions")
        val actionMenuItemAddCompany = MenuItem("Add company").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val idField = TextField()
                val titleField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (idField.text != "" && titleField.text != "") {
                            val company = Company(idField.text.toInt(), titleField.text.toString(), 0)
                            DataBase.addCompany(actualConnection!!, company)
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(Label("Add company dialog"), idField, titleField, buttons)
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val actionMenuItemAddOrder = MenuItem("Add order").apply {

            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val idField = TextField()
                val companyIdField = TextField()
                val descriptionField = TextField()
                val priceField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (idField.text != "" && companyIdField.text != ""
                            && descriptionField.text != "" && priceField.text != ""
                        ) {
                            val order = Order(
                                idField.text.toInt(),
                                companyIdField.text.toInt(),
                                descriptionField.text,
                                priceField.text.toInt()
                            )

                            DataBase.addOrder(actualConnection!!, order)
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Add order dialog"),
                    idField,
                    companyIdField,
                    descriptionField,
                    priceField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val actionMenuItemReplaceTable = MenuItem("Switch table").apply {
            onAction = EventHandler { switchTables() }
        }

        val searchMenu = Menu("Search")
        val searchMenuItemOrderByDescription = MenuItem("Find orders by description").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val titleField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (titleField.text != "") {
                            ordersTable.items.clear()
                            ordersTable.items.addAll(DataBase.findOrderByDescription(actualConnection!!, titleField.text))
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Find orders by description dialog"),
                    titleField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val searchMenuItemCompanyByTitle = MenuItem("Find companies by title").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val titleField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (titleField.text != "") {
                            companyTable.items.clear()
                            companyTable.items.addAll(DataBase.findCompanyByTitle(actualConnection!!, titleField.text))
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Find company by title dialog"),
                    titleField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val searchMenuItemShowClear = MenuItem("Show clear data").apply {
            onAction = EventHandler { refreshData() }
        }

        val removeMenu = Menu("Remove")
        val removeMenuItemOrderById = MenuItem("Remove order by id").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val idField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (idField.text != "") {
                            DataBase.deleteOrderById(actualConnection!!, idField.text.toInt())
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Remove order by id dialog"),
                    idField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val removeMenuItemOrderByDescriptions = MenuItem("Remove orders by descriptions").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val descriptionField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (descriptionField.text != "") {
                            DataBase.deleteOrderByDescription(actualConnection!!, descriptionField.text)
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Remove order by id dialog"),
                    descriptionField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val removeMenuItemCompanyById = MenuItem("Remove company by id").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val idField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (idField.text != "") {
                            DataBase.deleteCompanyById(actualConnection!!, idField.text.toInt())
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Remove company by id dialog"),
                    idField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }
        val removeMenuItemCompanyByTitle = MenuItem("Remove company by title").apply {
            onAction = EventHandler {
                val dialog = Stage()
                dialog.initStyle(StageStyle.UTILITY)
                val box = VBox()
                box.alignment = Pos.CENTER
                val buttons = HBox()
                buttons.alignment = Pos.CENTER

                val titleField = TextField()

                val btnOK = Button("Ok").apply {

                    onMouseClicked = EventHandler {
                        if (titleField.text != "") {
                            DataBase.deleteCompanyByTitle(actualConnection!!, titleField.text)
                            refreshData()
                            dialog.close()
                        }
                    }

                }
                val btnCancel = Button("Cancel").apply {
                    onMouseClicked = EventHandler { dialog.close() }
                }
                buttons.children.addAll(btnOK, btnCancel)

                box.children.addAll(
                    Label("Remove order by id dialog"),
                    titleField,
                    buttons
                )
                val scene = Scene(box)
                dialog.scene = scene
                dialog.show()
            }
        }

        val cleaningMenu = Menu("Cleaning")
        val cleanOrders = MenuItem("Clean orders").apply {
            onAction = EventHandler {
                DataBase.clearOrders(actualConnection!!)
            }
        }
        val cleanCompanies = MenuItem("Clean companies").apply {
            onAction = EventHandler {
                DataBase.clearCompanies(actualConnection!!)
            }
        }
        val cleanTables = MenuItem("Clean all tables").apply {
            onAction = EventHandler {
                DataBase.clearTables(actualConnection!!)
            }
        }

        val dangerousMenu = Menu("Dangerous")
        val deleteDatabase = MenuItem("Delete database").apply {
            onAction = EventHandler {
                isUserLogin = false

                companyTable.items.clear()
                ordersTable.items.clear()
            }
        }

        userMenu.items.addAll(userMenuItemChangeUser)
        actionMenu.items.addAll(actionMenuItemReplaceTable, actionMenuItemAddCompany, actionMenuItemAddOrder)
        searchMenu.items.addAll(searchMenuItemOrderByDescription, searchMenuItemCompanyByTitle, searchMenuItemShowClear)
        removeMenu.items.addAll(
            removeMenuItemOrderById, removeMenuItemOrderByDescriptions,
            removeMenuItemCompanyById, removeMenuItemCompanyByTitle
        )
        cleaningMenu.items.addAll(cleanOrders, cleanCompanies, cleanTables)
        dangerousMenu.items.add(deleteDatabase)

        menuBar.menus.addAll(userMenu, actionMenu, searchMenu, removeMenu, cleaningMenu, dangerousMenu)
        return menuBar
    }

    private fun switchTables() {
        if (isUserLogin) {
            if (isCompaniesShowed) {
                root.children.remove(companyTable)
                root.children.add(ordersTable)
            } else {
                root.children.remove(ordersTable)
                root.children.add(companyTable)
            }
            isCompaniesShowed = !isCompaniesShowed
        }
    }

    private fun refreshData() {
        companyTable.items.clear()
        companyTable.items.addAll(DataBase.getCompanies(actualConnection!!))
        ordersTable.items.clear()
        ordersTable.items.addAll(DataBase.getOrders(actualConnection!!))
    }
}

fun main() {
    Application.launch(MainView::class.java)
}