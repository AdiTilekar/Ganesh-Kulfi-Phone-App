package com.ganeshkulfi.app.presentation.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.model.User
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuest = viewModel.isGuestUser()
    val isAdmin = currentUser?.role == com.ganeshkulfi.app.data.model.UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Card
            currentUser?.let { user ->
                UserInfoCard(user = user, isGuest = isGuest)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profile Options
            if (isGuest) {
                GuestProfileOptions(
                    onNavigateToLogin = onNavigateToLogin
                )
            } else {
                RegisteredUserOptions(
                    onSignOut = {
                        viewModel.signOut()
                        onNavigateToLogin()
                    },
                    isAdmin = isAdmin,
                    onNavigateToAdmin = onNavigateToAdmin
                )
            }
        }
    }
}

@Composable
fun UserInfoCard(user: User, isGuest: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (isGuest) {
                Text(
                    text = "Guest Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (user.phone.isNotEmpty()) {
                    Text(
                        text = user.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GuestProfileOptions(
    onNavigateToLogin: () -> Unit
) {
    Text(
        text = "Guest Mode",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "You're browsing as a guest. Create an account to place orders and save your preferences.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onNavigateToLogin,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Person, "Create Account", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Account / Sign In")
    }
}

@Composable
fun RegisteredUserOptions(
    onSignOut: () -> Unit,
    isAdmin: Boolean = false,
    onNavigateToAdmin: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Text(
        text = "Account Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    
    // Admin Dashboard Access (only for admin users)
    if (isAdmin) {
        @OptIn(ExperimentalMaterial3Api::class)
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToAdmin,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AdminPanelSettings, "Admin Dashboard", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Manage inventory & retailers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, "Go", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { 
            scope.launch { 
                snackbarHostState.showSnackbar("Orders feature coming soon!") 
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ShoppingCart, "Orders")
            Spacer(modifier = Modifier.width(16.dp))
            Text("My Orders", modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, "Go")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { 
            scope.launch { 
                snackbarHostState.showSnackbar("Edit Profile feature coming soon!") 
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, "Edit Profile")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Edit Profile", modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, "Go")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { 
            scope.launch { 
                snackbarHostState.showSnackbar("Settings feature coming soon!") 
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, "Settings")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, "Go")
        }
    }
    
    // Snackbar Host
    SnackbarHost(hostState = snackbarHostState)

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(Icons.Default.ExitToApp, "Sign Out", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out")
    }
}
