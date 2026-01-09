package com.ganeshkulfi.app.presentation.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ganeshkulfi.app.presentation.ui.auth.LoginScreen
import com.ganeshkulfi.app.presentation.ui.auth.SignUpScreen
import com.ganeshkulfi.app.presentation.ui.cart.CartScreen
import com.ganeshkulfi.app.presentation.ui.home.HomeScreen
import com.ganeshkulfi.app.presentation.ui.orders.OrdersScreen
import com.ganeshkulfi.app.presentation.ui.profile.ProfileScreen
import com.ganeshkulfi.app.presentation.ui.splash.SplashScreen
import com.ganeshkulfi.app.presentation.ui.admin.AdminDashboardScreen
import com.ganeshkulfi.app.presentation.ui.admin.AdminOrdersScreen
import com.ganeshkulfi.app.presentation.ui.admin.InventoryManagementScreen
import com.ganeshkulfi.app.presentation.ui.admin.RetailerManagementScreen
import com.ganeshkulfi.app.presentation.screens.PricingManagementScreen
import com.ganeshkulfi.app.presentation.screens.ReportsAnalyticsScreen
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel
import com.ganeshkulfi.app.presentation.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Shared CartViewModel for retailer screens
    val cartViewModel: CartViewModel = hiltViewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRetailerHome = {
                    navController.navigate(Screen.RetailerHome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRetailerHome = {
                    navController.navigate(Screen.RetailerHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            val previousRoute = navController.previousBackStackEntry?.destination?.route
            val isFromAdmin = previousRoute == Screen.Admin.route
            
            HomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateBack = if (isFromAdmin) {
                    { 
                        navController.navigate(Screen.Admin.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                } else null,
                isAdminPreview = isFromAdmin
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }
        
        composable(Screen.Cart.route) {
            CartScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Orders.route) {
            OrdersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Admin.route) {
            AdminDashboardScreen(
                onNavigateToInventory = {
                    navController.navigate("admin/inventory")
                },
                onNavigateToRetailers = {
                    navController.navigate("admin/retailers")
                },
                onNavigateToPricing = {
                    navController.navigate("admin/pricing")
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.AdminOrders.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.AdminReports.route)
                },
                onNavigateBack = {
                    // Navigate to home screen (customer view) so admin can see what customers see
                    navController.navigate(Screen.Home.route)
                },
                onNavigateToRetailerView = {
                    // Navigate to retailer home screen so admin can see retailer ordering experience
                    navController.navigate(Screen.RetailerHome.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable("admin/inventory") {
            InventoryManagementScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo("admin/inventory") { inclusive = true }
                    }
                }
            )
        }
        
        composable("admin/retailers") {
            RetailerManagementScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo("admin/retailers") { inclusive = true }
                    }
                },
                onRetailerClick = { retailerId ->
                    // Retailer details screen not yet implemented
                    // Future: navController.navigate("admin/retailers/$retailerId")
                }
            )
        }
        
        composable("admin/pricing") {
            PricingManagementScreen(
                onBackClick = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo("admin/pricing") { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.AdminOrders.route) {
            AdminOrdersScreen(
                onBackClick = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo(Screen.AdminOrders.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminReports.route) {
            ReportsAnalyticsScreen(
                onBackClick = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo(Screen.AdminReports.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Retailer Routes - Modern Swiggy/Zomato Style
        composable(Screen.RetailerHome.route) {
            val previousRoute = navController.previousBackStackEntry?.destination?.route
            val isFromAdmin = previousRoute == Screen.Admin.route
            
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    com.ganeshkulfi.app.presentation.ui.retailer.RetailerDrawer(
                        onNavigateToOrders = {
                            navController.navigate(Screen.RetailerOrders.route)
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.RetailerProfile.route)
                        },
                        onSignOut = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onCloseDrawer = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                com.ganeshkulfi.app.presentation.ui.retailer.ProductCatalogScreen(
                    onNavigateToCart = {
                        navController.navigate(Screen.RetailerCart.route)
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    cartViewModel = cartViewModel,
                    onNavigateBack = if (isFromAdmin) {
                        { 
                            navController.navigate(Screen.Admin.route) {
                                popUpTo(Screen.RetailerHome.route) { inclusive = true }
                            }
                        }
                    } else null,
                    isAdminPreview = isFromAdmin
                )
            }
        }
        
        composable(Screen.RetailerCart.route) {
            com.ganeshkulfi.app.presentation.ui.retailer.CartScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOrderSuccess = { orderNumber ->
                    navController.navigate(Screen.RetailerOrderSuccess.createRoute(orderNumber)) {
                        popUpTo(Screen.RetailerHome.route)
                    }
                },
                cartViewModel = cartViewModel
            )
        }
        
        composable(
            route = Screen.RetailerOrderSuccess.route,
            arguments = listOf(navArgument("orderNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
            com.ganeshkulfi.app.presentation.ui.retailer.OrderSuccessScreen(
                orderNumber = orderNumber,
                onNavigateToHome = {
                    navController.navigate(Screen.RetailerHome.route) {
                        popUpTo(Screen.RetailerHome.route) { inclusive = true }
                    }
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.RetailerOrders.route) {
                        popUpTo(Screen.RetailerHome.route)
                    }
                }
            )
        }
        
        composable(Screen.RetailerOrders.route) {
            com.ganeshkulfi.app.presentation.ui.retailer.RetailerOrdersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.RetailerProfile.route) {
            com.ganeshkulfi.app.presentation.ui.retailer.RetailerProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
