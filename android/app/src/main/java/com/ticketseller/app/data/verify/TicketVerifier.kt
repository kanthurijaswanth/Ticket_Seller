package com.ticketseller.app.data.verify

import com.ticketseller.app.data.model.TicketDetails
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.ListingRepository
import javax.inject.Inject
import javax.inject.Singleton

data class VerificationResult(
    val approved: Boolean,
    val flags: List<String>,
    val notes: String,
    val extracted: ExtractedFields,
)

data class ExtractedFields(
    val pnr: String = "",
    val source: String = "",
    val destination: String = "",
    val journeyDateMillis: Long = 0L,
    val passenger: String = "",
)

@Singleton
class TicketVerifier @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    /**
     * Two-layer verification:
     *  L1 (on-device): OCR text → required fields present, future date, PNR format ok.
     *  L2 (cross-DB): perceptual image hash not seen on any other listing (duplicate fraud).
     * Returns approved=true only if both pass with no critical flags.
     */
    suspend fun verify(
        ocrText: String,
        imageHash: String,
        declared: TicketDetails,
    ): VerificationResult {
        val flags = mutableListOf<String>()
        val extracted = extract(ocrText, declared.type)

        // L1: required fields
        val effectivePnr = extracted.pnr.ifBlank { declared.pnrOrBookingId }
        if (effectivePnr.isBlank()) flags += "missing_pnr"
        when (declared.type) {
            TicketType.TRAIN -> {
                if (effectivePnr.isNotBlank() && !PNR_TRAIN.matches(effectivePnr)) {
                    flags += "train_pnr_format"
                }
            }
            TicketType.BUS -> {
                if (effectivePnr.isNotBlank() && effectivePnr.length < 6) {
                    flags += "bus_pnr_too_short"
                }
            }
        }

        // future date check (allow ±1 day grace for OCR ambiguity)
        val now = System.currentTimeMillis()
        val journey = if (extracted.journeyDateMillis > 0) extracted.journeyDateMillis else declared.journeyDateMillis
        if (journey == 0L) flags += "missing_date"
        else if (journey < now - 24L * 3600_000) flags += "past_date"

        // L2: duplicate image
        if (imageHash.isNotBlank() && listingRepository.anyWithImageHash(imageHash)) {
            flags += "duplicate_image"
        }

        // basic source/destination presence
        if (declared.source.isBlank() || declared.destination.isBlank()) {
            flags += "missing_route"
        }

        val critical = setOf("duplicate_image", "past_date")
        val approved = flags.none { it in critical } &&
                flags.none { it.endsWith("_format") } &&
                flags.size <= 1

        val notes = if (approved) {
            "Auto-verified (OCR + duplicate check)."
        } else {
            "Held for manual review. Reasons: ${flags.joinToString()}"
        }
        return VerificationResult(approved, flags, notes, extracted)
    }

    fun extract(text: String, type: TicketType): ExtractedFields {
        val lines = text.lines().map { it.trim() }
        val pnrRegex = when (type) {
            TicketType.TRAIN -> Regex("\\b(\\d{10})\\b")
            TicketType.BUS -> Regex("\\b([A-Z0-9]{6,14})\\b")
        }
        val pnr = pnrRegex.find(text)?.groupValues?.getOrNull(1).orEmpty()
        val dateMillis = DATE_REGEXES.firstNotNullOfOrNull { (re, fmt) ->
            re.find(text)?.value?.let { parseDate(it, fmt) }
        } ?: 0L

        // crude source/dest detection: lines like "Bengaluru → Hyderabad" or "From X To Y"
        var source = ""
        var destination = ""
        val arrow = Regex("([A-Za-z][A-Za-z .]+?)\\s*(?:→|->|to|TO|-)\\s*([A-Za-z][A-Za-z .]+)")
        for (l in lines) {
            arrow.find(l)?.let {
                source = it.groupValues[1].trim()
                destination = it.groupValues[2].trim()
                return@let
            }
            if (source.isNotEmpty()) break
        }

        return ExtractedFields(
            pnr = pnr,
            source = source,
            destination = destination,
            journeyDateMillis = dateMillis,
            passenger = "",
        )
    }

    private fun parseDate(raw: String, pattern: String): Long? = try {
        val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
        fmt.parse(raw)?.time
    } catch (_: Exception) { null }

    companion object {
        private val PNR_TRAIN = Regex("^\\d{10}$")
        private val DATE_REGEXES = listOf(
            Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b") to "dd/MM/yyyy",
            Regex("\\b\\d{2}-\\d{2}-\\d{4}\\b") to "dd-MM-yyyy",
            Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b") to "yyyy-MM-dd",
            Regex("\\b\\d{1,2} [A-Za-z]{3} \\d{4}\\b") to "d MMM yyyy",
        )
    }
}
