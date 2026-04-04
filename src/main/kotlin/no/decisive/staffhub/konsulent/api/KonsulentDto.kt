package no.decisive.staffhub.konsulent.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.konsulent.Kompetanse
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import no.decisive.staffhub.konsulent.Konsulent
import java.time.LocalDateTime

data class OpprettKonsulentRequest(
    @field:NotBlank(message = "fornavn er påkrevd")
    val fornavn: String,
    @field:NotBlank(message = "etternavn er påkrevd")
    val etternavn: String,
    @field:NotBlank(message = "epost er påkrevd")
    @field:Email(message = "epost må være en gyldig e-postadresse")
    val epost: String,
    val telefon: String? = null,
    @field:Valid
    val kompetanser: List<OpprettKompetanseRequest> = emptyList(),
)

data class OpprettKompetanseRequest(
    val fagområde: Fagområde,
    val nivå: Kompetansenivå,
    val beskrivelse: String? = null,
)

data class KonsulentResponse(
    val id: Long,
    val fornavn: String,
    val etternavn: String,
    val epost: String,
    val telefon: String?,
    val kompetanser: List<KompetanseResponse>,
    val opprettetDato: LocalDateTime,
)

data class KompetanseResponse(
    val id: Long,
    val fagområde: Fagområde,
    val nivå: Kompetansenivå,
    val beskrivelse: String?,
)

fun OpprettKonsulentRequest.tilDomene(idProvider: IdProvider): Konsulent {
    val kompetanseEntiteter = kompetanser.map { it.tilDomene(idProvider) }
    return Konsulent(
        idProvider = idProvider,
        fornavn = fornavn,
        etternavn = etternavn,
        epost = epost,
        telefon = telefon,
        kompetanser = kompetanseEntiteter
    )
}

fun OpprettKompetanseRequest.tilDomene(idProvider: IdProvider) = Kompetanse(
    idProvider = idProvider,
    fagområde = fagområde,
    nivå = nivå,
    beskrivelse = beskrivelse
)

fun Konsulent.tilResponse() = KonsulentResponse(
    id = id,
    fornavn = fornavn,
    etternavn = etternavn,
    epost = epost,
    telefon = telefon,
    kompetanser = kompetanser.map { it.tilResponse() },
    opprettetDato = opprettetDato
)

fun Kompetanse.tilResponse() = KompetanseResponse(
    id = id,
    fagområde = fagområde,
    nivå = nivå,
    beskrivelse = beskrivelse
)
