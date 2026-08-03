package com.ticketseller.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.screens.chat.ChatListPane
import com.ticketseller.app.ui.screens.home.HomePane
import com.ticketseller.app.ui.screens.orders.MyTicketsPane
import com.ticketseller.app.ui.screens.profile.ColoursThemeSheet
import com.ticketseller.app.ui.screens.profile.ProfilePane
import com.ticketseller.app.ui.theme.LocalGlass

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Tickets("Your Tickets", Icons.Filled.ConfirmationNumber),
    Chats("Chats", Icons.AutoMirrored.Filled.Chat),
    Profile("You", Icons.Filled.Person),
}

@Composable
fun MainShell(
    onSell: () -> Unit,
    onListing: (String) -> Unit,
    onChat: (String) -> Unit,
    onOrder: (String) -> Unit,
    onEditProfile: () -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var showTheme by remember { mutableStateOf(false) }

    if (showTheme) ColoursThemeSheet(onDismiss = { showTheme = false })

    AppBackground {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                Tab.Home -> HomePane(onListing = onListing, onSell = onSell, onNotifications = onNotifications, onOpenProfile = { tab = Tab.Profile })
                Tab.Tickets -> MyTicketsPane(onListing = onListing, onOrder = onOrder)
                Tab.Chats -> ChatListPane(onChat = onChat)
                Tab.Profile -> ProfilePane(onEditProfile = onEditProfile, onSignOut = onSignOut, onOpenTheme = { showTheme = true })
            }

            BottomNavBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun BottomNavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    // Full-width bar flush to the bottom edge — opaque, so nothing behind it can be tapped.
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(g.bg1)
            .background(g.glass)
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Tab.entries.forEach { t ->
                val active = t == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) g.tint else Color.Transparent)
                        .clickable { onSelect(t) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(t.icon, t.label, tint = if (active) g.tintInk else g.ink3, modifier = Modifier.size(20.dp))
                    Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (active) g.tintInk else g.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
