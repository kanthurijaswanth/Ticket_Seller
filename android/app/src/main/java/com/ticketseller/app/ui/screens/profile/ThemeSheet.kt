package com.ticketseller.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ticketseller.app.ui.common.InkButton
import com.ticketseller.app.ui.theme.LocalGlass
import com.ticketseller.app.ui.theme.ThemeMeta
import com.ticketseller.app.ui.theme.ThemeViewModel
import com.ticketseller.app.ui.theme.themeMetas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColoursThemeSheet(
    onDismiss: () -> Unit,
    themeVm: ThemeViewModel = hiltViewModel(),
) {
    val g = LocalGlass.current
    val c = themeVm.controller
    val dark = c.darkOverride ?: isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = g.sheet,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("Colours & theme", style = MaterialTheme.typography.headlineSmall, color = g.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Two colours per theme, so the app never gets noisy. Applies everywhere instantly.",
                style = MaterialTheme.typography.bodySmall, color = g.ink2,
            )

            // Light / Dark toggle
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(g.glass)
                    .border(1.dp, g.glassBorder, RoundedCornerShape(18.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ModeToggle("Light", Icons.Filled.LightMode, selected = !dark, modifier = Modifier.weight(1f)) { c.setDark(false) }
                ModeToggle("Dark", Icons.Filled.DarkMode, selected = dark, modifier = Modifier.weight(1f)) { c.setDark(true) }
            }

            // Theme swatches, 2 per row
            Spacer(Modifier.height(14.dp))
            themeMetas(dark).chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { meta ->
                        ThemeSwatch(meta, active = meta.key == c.themeKey, modifier = Modifier.weight(1f)) { c.setTheme(meta.key) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(8.dp))
            InkButton("Use this look", onClick = onDismiss)
        }
    }
}

@Composable
private fun ModeToggle(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    val bg = if (selected) g.ink else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) g.onInk else g.ink2
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ThemeSwatch(meta: ThemeMeta, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(g.glass)
            .border(2.dp, if (active) g.deep else androidx.compose.ui.graphics.Color.Transparent, shape)
            .clickable { onClick() }
            .padding(13.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(9.dp)).background(meta.brand))
            Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(9.dp)).background(meta.deep))
            Box(Modifier.width(14.dp).height(30.dp).clip(RoundedCornerShape(9.dp)).background(meta.ink))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(meta.name, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
            if (active) {
                Spacer(Modifier.width(6.dp))
                Text("· on", style = MaterialTheme.typography.labelSmall, color = g.accent)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(meta.sub, style = MaterialTheme.typography.labelSmall, color = g.ink3, fontWeight = FontWeight.Medium)
    }
}
