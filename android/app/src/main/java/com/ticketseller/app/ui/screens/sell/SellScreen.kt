package com.ticketseller.app.ui.screens.sell

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ticketseller.app.data.model.TicketType
import androidx.compose.ui.draw.shadow
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassIconButton
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.common.glassBox
import com.ticketseller.app.ui.theme.LocalGlass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SellScreen(
    vm: SellViewModel = hiltViewModel(),
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val g = LocalGlass.current
    val accent = g.accent
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.processImage(context, it) }
    }

    LaunchedEffect(ui.successId) { if (ui.successId != null) onDone() }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Text("Sell a ticket", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
            }

            SectionTitle("Ticket type")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeChip("Bus", Icons.Filled.DirectionsBus, ui.type == TicketType.BUS, Modifier.weight(1f)) { vm.setType(TicketType.BUS) }
                TypeChip("Train", Icons.Filled.Train, ui.type == TicketType.TRAIN, Modifier.weight(1f)) { vm.setType(TicketType.TRAIN) }
            }

            SectionTitle("Ticket photo")
            Text("We auto-read the details from your photo and check it isn't a duplicate. Hide anything you don't want public.", style = MaterialTheme.typography.bodySmall, color = g.ink3)
            val boxShape = RoundedCornerShape(20.dp)
            Box(
                Modifier.fillMaxWidth().height(190.dp).shadow(6.dp, boxShape, ambientColor = g.shadow, spotColor = g.shadow).clip(boxShape).background(g.glass)
                    .drawBehind {
                        drawRoundRect(
                            color = accent,
                            topLeft = androidx.compose.ui.geometry.Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                            cornerRadius = CornerRadius(18.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 14f))),
                        )
                    }
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                val bmp = ui.imageBitmap
                when {
                    bmp != null -> Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize().clip(boxShape))
                    ui.processingImage -> Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = g.deep); Spacer(Modifier.height(8.dp)); Text("Reading your ticket…", color = g.ink3, style = MaterialTheme.typography.bodySmall) }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(40.dp), tint = accent)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to upload your ticket photo", color = g.ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Screenshot or photo — JPG / PNG", color = g.ink3, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            GlassAction(if (ui.imageBitmap == null) "Upload ticket image" else "Replace image") { pickImage.launch("image/*") }

            SectionTitle("Journey details")
            val t = ui.ticket
            GlassInput("From (source city)", t.source) { vm.setTicket(t.copy(source = it)) }
            GlassInput("To (destination city)", t.destination) { vm.setTicket(t.copy(destination = it)) }
            GlassInput(if (ui.type == TicketType.BUS) "Bus operator" else "Train name / operator", t.operator) { vm.setTicket(t.copy(operator = it)) }
            GlassInput(if (ui.type == TicketType.BUS) "Bus number / route code" else "Train number", t.trainOrBusNumber) { vm.setTicket(t.copy(trainOrBusNumber = it)) }
            GlassInput("PNR / Booking ID", t.pnrOrBookingId) { vm.setTicket(t.copy(pnrOrBookingId = it)) }
            GlassInput("Seat / coach", t.seatOrCoach) { vm.setTicket(t.copy(seatOrCoach = it)) }
            GlassInput("Passenger name (as on ticket)", t.passengerName) { vm.setTicket(t.copy(passengerName = it)) }
            DateTimeField(t.journeyDateMillis, t.departureTime) { ms, tm -> vm.setTicket(t.copy(journeyDateMillis = ms, departureTime = tm)) }

            SectionTitle("Pickup & drop")
            val a = ui.address
            GlassInput("Pickup address / boarding point", a.pickupLine1) { vm.setAddress(a.copy(pickupLine1 = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassInput("Pickup city", a.pickupCity, Modifier.weight(1f)) { vm.setAddress(a.copy(pickupCity = it)) }
                GlassInput("Pincode", a.pickupPincode, Modifier.weight(1f), KeyboardType.Number) { vm.setAddress(a.copy(pickupPincode = it.filter(Char::isDigit))) }
            }
            GlassInput("Drop address / station", a.dropLine1) { vm.setAddress(a.copy(dropLine1 = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassInput("Drop city", a.dropCity, Modifier.weight(1f)) { vm.setAddress(a.copy(dropCity = it)) }
                GlassInput("Pincode", a.dropPincode, Modifier.weight(1f), KeyboardType.Number) { vm.setAddress(a.copy(dropPincode = it.filter(Char::isDigit))) }
            }
            GlassInput("Notes for buyer (optional)", a.notes) { vm.setAddress(a.copy(notes = it)) }

            SectionTitle("Asking price")
            GlassInput("Price in ₹", ui.priceRupees, keyboard = KeyboardType.Number, onValue = vm::setPrice)

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.height(6.dp))
            GradientButton(if (ui.submitting) "Submitting…" else "Post my ticket", enabled = !ui.submitting, onClick = vm::submit)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val g = LocalGlass.current
    Text(text, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun TypeChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) g.tint else g.glass).border(if (selected) 2.dp else 1.dp, if (selected) g.deep else g.glassBorder, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) g.tintInk else g.ink2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) g.tintInk else g.ink2)
    }
}

@Composable
private fun GlassInput(label: String, value: String, modifier: Modifier = Modifier, keyboard: KeyboardType = KeyboardType.Text, onValue: (String) -> Unit) {
    val g = LocalGlass.current
    Column(modifier.glassBox(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = g.ink3)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value, onValueChange = onValue, singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = g.ink, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(g.deep),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GlassAction(text: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier.fillMaxWidth().glassBox(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = g.ink, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun DateTimeField(millis: Long, timeStr: String, onChange: (Long, String) -> Unit) {
    val g = LocalGlass.current
    val context = LocalContext.current
    val display = if (millis > 0) SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(Date(millis)) + (if (timeStr.isNotBlank()) " · $timeStr" else "") else "Pick date and time"
    Column(
        Modifier.fillMaxWidth().glassBox(RoundedCornerShape(16.dp)).clickable {
            val cal = Calendar.getInstance()
            if (millis > 0) cal.timeInMillis = millis
            DatePickerDialog(context, { _, y, m, d ->
                cal.set(y, m, d)
                TimePickerDialog(context, { _, hh, mm ->
                    cal.set(Calendar.HOUR_OF_DAY, hh); cal.set(Calendar.MINUTE, mm)
                    onChange(cal.timeInMillis, String.format(Locale.ENGLISH, "%02d:%02d", hh, mm))
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }.padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text("JOURNEY DATE & TIME", style = MaterialTheme.typography.labelSmall, color = g.ink3)
        Spacer(Modifier.height(5.dp))
        Text(display, style = MaterialTheme.typography.titleMedium, color = if (millis > 0) g.ink else g.ink3, fontWeight = FontWeight.SemiBold)
    }
}
