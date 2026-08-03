package com.ticketseller.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    Tickets("Tickets", Icons.Filled.ConfirmationNumber),
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
                Tab.Home -> HomePane(onListing = onListing, onSell = onSell, onNotifications = onNotifications)
                Tab.Tickets -> MyTicketsPane(onListing = onListing, onOrder = onOrder)
                Tab.Chats -> ChatListPane(onChat = onChat)
                Tab.Profile -> ProfilePane(onEditProfile = onEditProfile, onSignOut = onSignOut, onOpenTheme = { showTheme = true })
            }

            GlassBottomBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun GlassBottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(g.bg1)          // opaque base — content doesn't show through
            .background(g.glass)        // subtle theme tint on top
            .border(1.dp, g.glassBorder, shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { t ->
            val active = t == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) g.tint else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(t.icon, t.label, tint = if (active) g.tintInk else g.ink3, modifier = Modifier.size(21.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (active) g.tintInk else g.ink3)
            }
        }
    }
}
