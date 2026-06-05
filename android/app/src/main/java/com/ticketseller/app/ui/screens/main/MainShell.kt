package com.ticketseller.app.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ticketseller.app.ui.screens.browse.BrowsePane
import com.ticketseller.app.ui.screens.chat.ChatListPane
import com.ticketseller.app.ui.screens.orders.MyTicketsPane
import com.ticketseller.app.ui.screens.profile.ProfilePane

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Browse("Browse", Icons.Outlined.Search),
    Tickets("My Tickets", Icons.Outlined.ConfirmationNumber),
    Chats("Chats", Icons.AutoMirrored.Filled.Chat),
    Profile("Profile", Icons.Outlined.AccountCircle),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onSell: () -> Unit,
    onListing: (String) -> Unit,
    onChat: (String) -> Unit,
    onOrder: (String) -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.Browse) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.label) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                for (t in Tab.values()) {
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Browse) {
                ExtendedFloatingActionButton(
                    onClick = onSell,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Sell ticket") },
                )
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Browse -> BrowsePane(onListing = onListing)
                Tab.Tickets -> MyTicketsPane(onListing = onListing, onOrder = onOrder)
                Tab.Chats -> ChatListPane(onChat = onChat)
                Tab.Profile -> ProfilePane(onEditProfile = onEditProfile, onSignOut = onSignOut)
            }
        }
    }
}
