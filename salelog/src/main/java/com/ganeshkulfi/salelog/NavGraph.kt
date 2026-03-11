package com.ganeshkulfi.salelog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganeshkulfi.salelog.ui.CartScreen
import com.ganeshkulfi.salelog.ui.ProductsScreen
import com.ganeshkulfi.salelog.ui.ReportsScreen

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Products : Screen("products", "Products", Icons.Default.Store)
    object Reports  : Screen("reports",  "Reports",  Icons.Default.Assessment)
    object Cart     : Screen("cart",     "Cart",     Icons.Default.Store)
}

private val bottomTabs = listOf(Screen.Products, Screen.Reports)

@Composable
fun SaleLogNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute != Screen.Cart.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected     = currentRoute == tab.route,
                            onClick      = {
                                navController.navigate(tab.route) {
                                    popUpTo(Screen.Products.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon         = { Icon(tab.icon, contentDescription = tab.label) },
                            label        = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Products.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Products.route) {
                ProductsScreen(onCartClick = { navController.navigate(Screen.Cart.route) })
            }
            composable(Screen.Cart.route) {
                CartScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
        }
    }
}
