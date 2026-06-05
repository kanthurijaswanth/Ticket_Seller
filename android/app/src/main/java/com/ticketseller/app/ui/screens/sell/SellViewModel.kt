package com.ticketseller.app.ui.screens.sell

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.ListingStatus
import com.ticketseller.app.data.model.PickupDropAddress
import com.ticketseller.app.data.model.TicketDetails
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.data.verify.ImageHasher
import com.ticketseller.app.data.verify.ImageUtils
import com.ticketseller.app.data.verify.OcrService
import com.ticketseller.app.data.verify.TicketVerifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SellUi(
    val type: TicketType = TicketType.BUS,
    val imageBitmap: Bitmap? = null,
    val imageBase64: String = "",
    val imageHash: String = "",
    val ocrText: String = "",
    val processingImage: Boolean = false,
    val ticket: TicketDetails = TicketDetails(),
    val address: PickupDropAddress = PickupDropAddress(),
    val priceRupees: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val successId: String? = null,
    val verificationFlags: List<String> = emptyList(),
    val verificationNote: String = "",
)

@HiltViewModel
class SellViewModel @Inject constructor(
    private val ocrService: OcrService,
    private val verifier: TicketVerifier,
    private val listingRepository: ListingRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SellUi())
    val ui: StateFlow<SellUi> = _ui

    fun setType(t: TicketType) { _ui.value = _ui.value.copy(type = t, ticket = _ui.value.ticket.copy(type = t)) }
    fun setTicket(td: TicketDetails) { _ui.value = _ui.value.copy(ticket = td) }
    fun setAddress(a: PickupDropAddress) { _ui.value = _ui.value.copy(address = a) }
    fun setPrice(p: String) { _ui.value = _ui.value.copy(priceRupees = p.filter { it.isDigit() }) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(processingImage = true, error = null)
            val (bmp, b64, hash, text, extracted) = withContext(Dispatchers.IO) {
                val bitmap = ImageUtils.loadAndCompress(context, uri)
                    ?: return@withContext Tuple5(null, "", "", "", null)
                val base64 = ImageUtils.bitmapToBase64(bitmap)
                val phash = ImageHasher.pHash(bitmap)
                val text = runCatching { ocrService.recognize(bitmap) }.getOrDefault("")
                val ex = verifier.extract(text, _ui.value.type)
                Tuple5(bitmap, base64, phash, text, ex)
            }
            if (bmp == null) {
                _ui.value = _ui.value.copy(processingImage = false, error = "Couldn't read image. Try another.")
                return@launch
            }
            val current = _ui.value.ticket
            val prefilled = current.copy(
                pnrOrBookingId = current.pnrOrBookingId.ifBlank { extracted?.pnr.orEmpty() },
                source = current.source.ifBlank { extracted?.source.orEmpty() },
                destination = current.destination.ifBlank { extracted?.destination.orEmpty() },
                journeyDateMillis = if (current.journeyDateMillis == 0L) extracted?.journeyDateMillis ?: 0L else current.journeyDateMillis,
            )
            _ui.value = _ui.value.copy(
                processingImage = false,
                imageBitmap = bmp,
                imageBase64 = b64,
                imageHash = hash,
                ocrText = text,
                ticket = prefilled,
            )
        }
    }

    fun submit() {
        val u = _ui.value
        val priceMinor = (u.priceRupees.toLongOrNull() ?: 0L) * 100
        val uid = authRepository.effectiveUid()
        if (uid.isNullOrBlank()) { _ui.value = u.copy(error = "Please sign in again."); return }
        if (u.imageBase64.isBlank()) { _ui.value = u.copy(error = "Please upload a clear ticket photo."); return }
        if (priceMinor <= 0) { _ui.value = u.copy(error = "Enter an asking price."); return }
        if (u.ticket.source.isBlank() || u.ticket.destination.isBlank()) {
            _ui.value = u.copy(error = "Enter source and destination."); return
        }
        if (u.ticket.journeyDateMillis == 0L) { _ui.value = u.copy(error = "Enter journey date."); return }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(submitting = true, error = null)
            // Wrap the entire pipeline so any failure (verifier, user fetch, Firestore write)
            // surfaces as a friendly error instead of dropping the user back to Browse.
            runCatching {
                val result = verifier.verify(u.ocrText, u.imageHash, u.ticket)
                // userRepository.get() is best-effort — failures shouldn't block submission.
                val me = runCatching { userRepository.get(uid) }.getOrNull()
                val status = if (result.approved) ListingStatus.ACTIVE else ListingStatus.PENDING_REVIEW
                val listing = Listing(
                    sellerId = uid,
                    sellerName = me?.name.orEmpty(),
                    sellerRating = me?.ratingAvg ?: 0.0,
                    ticket = u.ticket.copy(type = u.type),
                    address = u.address,
                    askingPrice = priceMinor,
                    ticketImageBase64 = u.imageBase64,
                    ticketImageHash = u.imageHash,
                    verificationNotes = result.notes,
                    verificationFlags = result.flags,
                    status = status,
                )
                val id = listingRepository.create(listing)
                Triple(id, result.flags, result.notes)
            }
                .onSuccess { (id, flags, notes) ->
                    _ui.value = _ui.value.copy(
                        submitting = false,
                        successId = id,
                        verificationFlags = flags,
                        verificationNote = notes,
                    )
                }
                .onFailure { e ->
                    val msg = e.message.orEmpty()
                    val friendly = when {
                        "PERMISSION_DENIED" in msg ->
                            "Couldn't save listing. Open Firebase Console → Firestore Database → Create database (test mode)."
                        "Cloud Firestore API has not been used" in msg ->
                            "Firestore isn't enabled for this project yet. Enable it at " +
                                "console.firebase.google.com → your project → Firestore Database → Create database."
                        msg.isBlank() -> "Submission failed. Please check your connection."
                        else -> "Submission failed: $msg"
                    }
                    _ui.value = _ui.value.copy(submitting = false, error = friendly)
                }
        }
    }

    private data class Tuple5(
        val bmp: Bitmap?,
        val b64: String,
        val hash: String,
        val text: String,
        val extracted: com.ticketseller.app.data.verify.ExtractedFields?,
    )
}
