package no.decisive.staffhub.oppdrag

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.UgyldigStatusOvergangException
import no.decisive.staffhub.felles.ValideringException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class Oppdrag private constructor(
    val id: Long,
    tittel: String,
    kundeNavn: String,
    beskrivelse: String?,
    startDato: LocalDate,
    sluttDato: LocalDate,
    status: Status,
    timepris: BigDecimal,
    val konsulentId: Long,
    val opprettetDato: LocalDateTime,
    versjon: Int,
) {

    var tittel: String = tittel
        set(value) { field = value; versjon++ }
    var kundeNavn: String = kundeNavn
        set(value) { field = value; versjon++ }
    var beskrivelse: String? = beskrivelse
        set(value) { field = value; versjon++ }
    var startDato: LocalDate = startDato
        private set
    var sluttDato: LocalDate = sluttDato
        private set
    var status: Status = status
        private set
    var timepris: BigDecimal = timepris
        private set
    var versjon: Int = versjon
        private set

    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState
    val persistertVersjon: Int? get() = persistertState?.versjon

    constructor(
        idProvider: IdProvider,
        tittel: String,
        kundeNavn: String,
        beskrivelse: String? = null,
        startDato: LocalDate,
        sluttDato: LocalDate,
        timepris: BigDecimal,
        konsulentId: Long,
    ) : this(
        id = idProvider.nesteOppdragId(),
        tittel = tittel,
        kundeNavn = kundeNavn,
        beskrivelse = beskrivelse,
        startDato = startDato,
        sluttDato = sluttDato,
        status = Status.FORESLÅTT,
        timepris = timepris,
        konsulentId = konsulentId,
        opprettetDato = LocalDateTime.now(),
        versjon = 1,
    ) {
        validerDatoer()
        validerTimepris()
    }

    fun endreStatus(nyStatus: Status) {
        if (!status.kanGåTil(nyStatus)) {
            throw UgyldigStatusOvergangException(
                "Kan ikke endre status fra $status til $nyStatus"
            )
        }
        status = nyStatus
        versjon++
    }

    fun endrePeriode(startDato: LocalDate, sluttDato: LocalDate) {
        this.startDato = startDato
        this.sluttDato = sluttDato
        validerDatoer()
        versjon++
    }

    fun endreTimepris(timepris: BigDecimal) {
        this.timepris = timepris
        validerTimepris()
        versjon++
    }

    fun overlapper(annet: Oppdrag): Boolean =
        !startDato.isAfter(annet.sluttDato) && !sluttDato.isBefore(annet.startDato)

    fun bekreftPersistert() {
        this.persistertState = tilState()
    }

    private fun validerDatoer() {
        if (!sluttDato.isAfter(startDato)) {
            throw ValideringException("sluttDato må være etter startDato")
        }
    }

    private fun validerTimepris() {
        if (timepris <= BigDecimal.ZERO) {
            throw ValideringException("timepris må være positiv")
        }
    }

    private fun tilState() = PersistertState(
        id, tittel, kundeNavn, beskrivelse, startDato, sluttDato, status, timepris, konsulentId, opprettetDato, versjon,
    )

    companion object {
        fun fra(state: PersistertState): Oppdrag {
            return Oppdrag(
                id = state.id,
                tittel = state.tittel,
                kundeNavn = state.kundeNavn,
                beskrivelse = state.beskrivelse,
                startDato = state.startDato,
                sluttDato = state.sluttDato,
                status = state.status,
                timepris = state.timepris,
                konsulentId = state.konsulentId,
                opprettetDato = state.opprettetDato,
                versjon = state.versjon,
            ).apply {
                bekreftPersistert()
            }
        }
    }

    enum class Status {
        FORESLÅTT,
        BEKREFTET,
        AKTIV,
        FULLFØRT,
        KANSELLERT,
        ;

        fun gyldigeOverganger(): Set<Status> = when (this) {
            FORESLÅTT -> setOf(BEKREFTET, KANSELLERT)
            BEKREFTET -> setOf(AKTIV, KANSELLERT)
            AKTIV -> setOf(FULLFØRT, KANSELLERT)
            FULLFØRT -> emptySet()
            KANSELLERT -> emptySet()
        }

        fun kanGåTil(nyStatus: Status): Boolean = nyStatus in gyldigeOverganger()
    }

    data class PersistertState(
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
        val versjon: Int,
    )
}
