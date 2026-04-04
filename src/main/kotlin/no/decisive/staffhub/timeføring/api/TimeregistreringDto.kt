package no.decisive.staffhub.timeføring.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.timeføring.Timeregistrering
import java.time.LocalDate
import java.time.LocalDateTime

data class OpprettTimeregistreringRequest(
    @field:NotNull(message = "oppdragId er påkrevd")
    val oppdragId: Long,
    @field:NotNull(message = "konsulentId er påkrevd")
    val konsulentId: Long,
    @field:NotNull(message = "dato er påkrevd")
    val dato: LocalDate,
    @field:NotNull(message = "timer er påkrevd")
    val timer: Int,
    @field:NotNull(message = "minutter er påkrevd")
    val minutter: Int,
    @field:NotBlank(message = "beskrivelse er påkrevd")
    val beskrivelse: String,
)

data class TimeregistreringResponse(
    val id: Long,
    val oppdragId: Long,
    val konsulentId: Long,
    val dato: LocalDate,
    val timer: Int,
    val minutter: Int,
    val beskrivelse: String,
    val totalMinutter: Int,
    val opprettetDato: LocalDateTime,
)

fun OpprettTimeregistreringRequest.tilDomene(idProvider: IdProvider) = Timeregistrering(
    idProvider = idProvider,
    oppdragId = oppdragId,
    konsulentId = konsulentId,
    dato = dato,
    timer = timer,
    minutter = minutter,
    beskrivelse = beskrivelse
)

fun Timeregistrering.tilResponse() = TimeregistreringResponse(
    id = id,
    oppdragId = oppdragId,
    konsulentId = konsulentId,
    dato = dato,
    timer = timer,
    minutter = minutter,
    beskrivelse = beskrivelse,
    totalMinutter = totalMinutter,
    opprettetDato = opprettetDato
)
