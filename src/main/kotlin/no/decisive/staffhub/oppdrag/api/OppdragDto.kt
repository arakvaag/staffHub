package no.decisive.staffhub.oppdrag.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.oppdrag.Oppdrag
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class OpprettOppdragRequest(
    @field:NotBlank(message = "tittel er påkrevd")
    val tittel: String,
    @field:NotBlank(message = "kundeNavn er påkrevd")
    val kundeNavn: String,
    val beskrivelse: String? = null,
    @field:NotNull(message = "startDato er påkrevd")
    val startDato: LocalDate,
    @field:NotNull(message = "sluttDato er påkrevd")
    val sluttDato: LocalDate,
    @field:NotNull(message = "timepris er påkrevd")
    @field:Positive(message = "timepris må være positiv")
    val timepris: BigDecimal,
    @field:NotNull(message = "konsulentId er påkrevd")
    val konsulentId: Long,
)

data class EndreStatusRequest(
    @field:NotNull(message = "nyStatus er påkrevd")
    val nyStatus: Status,
)

data class OppdragResponse(
    val id: Long,
    val tittel: String,
    val kundeNavn: String,
    val beskrivelse: String?,
    val startDato: LocalDate,
    val sluttDato: LocalDate,
    val status: Status,
    val timepris: BigDecimal,
    val konsulentId: Long,
    val opprettetDato: LocalDateTime,
)

fun OpprettOppdragRequest.tilDomene(idProvider: IdProvider) = Oppdrag(
    idProvider = idProvider,
    tittel = tittel,
    kundeNavn = kundeNavn,
    beskrivelse = beskrivelse,
    startDato = startDato,
    sluttDato = sluttDato,
    timepris = timepris,
    konsulentId = konsulentId
)

fun Oppdrag.tilResponse() = OppdragResponse(
    id = id,
    tittel = tittel,
    kundeNavn = kundeNavn,
    beskrivelse = beskrivelse,
    startDato = startDato,
    sluttDato = sluttDato,
    status = status,
    timepris = timepris,
    konsulentId = konsulentId,
    opprettetDato = opprettetDato
)
