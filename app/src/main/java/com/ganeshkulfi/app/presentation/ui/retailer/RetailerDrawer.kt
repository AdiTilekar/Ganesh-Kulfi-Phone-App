package com.ganeshkulfi.app.presentation.ui.retailer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel

@Composable
fun RetailerDrawer(
    onNavigateToOrders: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit,
    onCloseDrawer: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // User Name
                    Text(
                        text = currentUser?.name ?: "Retailer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Shop Name
                    if (!currentUser?.shopName.isNullOrBlank()) {
                        Text(
                            text = currentUser?.shopName ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Menu Items
            DrawerMenuItem(
                icon = Icons.Default.ListAlt,
                title = "My Orders",
                onClick = {
                    onNavigateToOrders()
                    onCloseDrawer()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.AccountCircle,
                title = "My Profile",
                onClick = {
                    onNavigateToProfile()
                    onCloseDrawer()
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            DrawerMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                onClick = {
                    onSignOut()
                    onCloseDrawer()
                },
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error
            )
            
            Spacer(Modifier.weight(1f))
            
            // Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ganesh Kulfi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Premium Quality • Fresh Daily",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        label = {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    )
}
