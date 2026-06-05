package com.ticketseller.app.ui.screens.sell

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ticketseller.app.data.model.TicketType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    vm: SellViewModel = hiltViewModel(),
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.processImage(context, it) }
    }

    LaunchedEffect(ui.successId) { if (ui.successId != null) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sell a ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.submit() },
                    enabled = !ui.submitting,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(if (ui.submitting) "Submitting…" else "Submit listing")
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ticket type", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = ui.type == TicketType.BUS,
                    onClick = { vm.setType(TicketType.BUS) },
                    leadingIcon = { Icon(Icons.Filled.DirectionsBus, null) },
                    label = { Text("Bus") },
                )
                FilterChip(
                    selected = ui.type == TicketType.TRAIN,
                    onClick = { vm.setType(TicketType.TRAIN) },
                    leadingIcon = { Icon(Icons.Filled.Train, null) },
                    label = { Text("Train") },
                )
            }

            Divider()
            Text("Ticket photo", style = MaterialTheme.typography.titleMedium)
            Text(
                "We auto-read details from the photo and check it isn't a duplicate. Hide personal info you don't want public — buyer details share only after you chat & agree.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val bmp = ui.imageBitmap
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (ui.processingImage) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PhotoCamera, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text("Tap below to upload", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { pickImage.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (ui.imageBitmap == null) "Upload ticket image" else "Replace image") }

            Divider()
            Text("Journey details", style = MaterialTheme.typography.titleMedium)
            val t = ui.ticket
            FieldText("From (source city)", t.source) { vm.setTicket(t.copy(source = it)) }
            FieldText("To (destination city)", t.destination) { vm.setTicket(t.copy(destination = it)) }
            FieldText(
                if (ui.type == TicketType.BUS) "Bus operator" else "Train name / operator",
                t.operator,
            ) { vm.setTicket(t.copy(operator = it)) }
            FieldText(
                if (ui.type == TicketType.BUS) "Bus number / route code" else "Train number",
                t.trainOrBusNumber,
            ) { vm.setTicket(t.copy(trainOrBusNumber = it)) }
            FieldText("PNR / Booking ID", t.pnrOrBookingId) { vm.setTicket(t.copy(pnrOrBookingId = it)) }
            FieldText("Seat / coach", t.seatOrCoach) { vm.setTicket(t.copy(seatOrCoach = it)) }
            FieldText("Passenger name (as on ticket)", t.passengerName) { vm.setTicket(t.copy(passengerName = it)) }
            DateTimePicker(
                label = "Journey date & time",
                millis = t.journeyDateMillis,
                timeStr = t.departureTime,
                onChange = { ms, tm -> vm.setTicket(t.copy(journeyDateMillis = ms, departureTime = tm)) },
            )

            Divider()
            Text("Pickup & drop", style = MaterialTheme.typography.titleMedium)
            Text(
                "Where the boarding point is, and where you'll get off. Helps buyers nearby find your ticket.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val a = ui.address
            FieldText("Pickup address / boarding point", a.pickupLine1) { vm.setAddress(a.copy(pickupLine1 = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldText("Pickup city", a.pickupCity, Modifier.weight(1f)) { vm.setAddress(a.copy(pickupCity = it)) }
                FieldText("Pincode", a.pickupPincode, Modifier.weight(1f), keyboard = KeyboardType.Number) {
                    vm.setAddress(a.copy(pickupPincode = it.filter(Char::isDigit)))
                }
            }
            FieldText("Pickup landmark (optional)", a.pickupLandmark) { vm.setAddress(a.copy(pickupLandmark = it)) }
            FieldText("Drop address / station", a.dropLine1) { vm.setAddress(a.copy(dropLine1 = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldText("Drop city", a.dropCity, Modifier.weight(1f)) { vm.setAddress(a.copy(dropCity = it)) }
                FieldText("Pincode", a.dropPincode, Modifier.weight(1f), keyboard = KeyboardType.Number) {
                    vm.setAddress(a.copy(dropPincode = it.filter(Char::isDigit)))
                }
            }
            FieldText("Notes for buyer (optional)", a.notes) { vm.setAddress(a.copy(notes = it)) }

            Divider()
            Text("Asking price", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = ui.priceRupees,
                onValueChange = vm::setPrice,
                label = { Text("Price in ₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FieldText(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DateTimePicker(
    label: String,
    millis: Long,
    timeStr: String,
    onChange: (Long, String) -> Unit,
) {
    val context = LocalContext.current
    val display = if (millis > 0) {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(Date(millis)) +
            (if (timeStr.isNotBlank()) " · $timeStr" else "")
    } else "Pick date and time"
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance()
            if (millis > 0) cal.timeInMillis = millis
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    TimePickerDialog(
                        context,
                        { _, hh, mm ->
                            cal.set(Calendar.HOUR_OF_DAY, hh)
                            cal.set(Calendar.MINUTE, mm)
                            val ts = String.format(Locale.ENGLISH, "%02d:%02d", hh, mm)
                            onChange(cal.timeInMillis, ts)
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true,
                    ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (millis > 0) display else label)
    }
}
