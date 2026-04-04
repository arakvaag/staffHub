package no.decisive.staffhub.timeføring

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.ValideringException
import java.time.LocalDate
import java.time.LocalDateTime

class Timeregistrering private constructor(
    val id: Long,
    val oppdragId: Long,
    val konsulentId: Long,
    val dato: LocalDate,
    val timer: Int,
    val minutter: Int,
    val beskrivelse: String,
    val opprettetDato: LocalDateTime,
) {
    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState

    constructor(
        idProvider: IdProvider,
        oppdragId: Long,
        konsulentId: Long,
        dato: LocalDate,
        timer: Int,
        minutter: Int,
        beskrivelse: String,
    ) : this(
        id = idProvider.nesteTimeregistreringId(),
        oppdragId = oppdragId,
        konsulentId = konsulentId,
        dato = dato,
        timer = timer,
        minutter = minutter,
        beskrivelse = beskrivelse,
        opprettetDato = LocalDateTime.now(),
    ) {
        valider()
    }

    val totalMinutter: Int get() = timer * 60 + minutter

    fun bekreftPersistert() {
        this.persistertState = tilState()
    }

    private fun valider() {
        if (timer !in 0..23) {
            throw ValideringException("timer må være mellom 0 og 23")
        }
        if (minutter !in 0..59) {
            throw ValideringException("minutter må være mellom 0 og 59")
        }
        if (timer == 0 && minutter == 0) {
            throw ValideringException("Det må registreres minst 1 minutt")
        }
    }

    private fun tilState() = PersistertState(
        id, oppdragId, konsulentId, dato, timer, minutter, beskrivelse, opprettetDato,
    )

    companion object {
        fun fra(state: PersistertState) = Timeregistrering(
            id = state.id,
            oppdragId = state.oppdragId,
            konsulentId = state.konsulentId,
            dato = state.dato,
            timer = state.timer,
            minutter = state.minutter,
            beskrivelse = state.beskrivelse,
            opprettetDato = state.opprettetDato,
        ).apply {
            bekreftPersistert()
        }
    }

    data class PersistertState(
        val id: Long,
        val oppdragId: Long,
        val konsulentId: Long,
        val dato: LocalDate,
        val timer: Int,
        val minutter: Int,
        val beskrivelse: String,
        val opprettetDato: LocalDateTime,
    )
}
