package com.ganeshkulfi.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object FlavorDetail : Screen("flavor_detail/{flavorId}") {
        fun createRoute(flavorId: String) = "flavor_detail/$flavorId"
    }
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object Orders : Screen("orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object Profile : Screen("profile")
    object Admin : Screen("admin")
    object AdminOrders : Screen("admin/orders")
    object AdminMenu : Screen("admin/menu")
    object AdminReports : Screen("admin/reports")
    object RetailerHome : Screen("retailer/home")  // Now ProductCatalogScreen
    object RetailerCart : Screen("retailer/cart")
    object RetailerOrderSuccess : Screen("retailer/order-success/{orderNumber}") {
        fun createRoute(orderNumber: String) = "retailer/order-success/$orderNumber"
    }
    object RetailerOrders : Screen("retailer/orders")
    object RetailerProfile : Screen("retailer/profile")
}
