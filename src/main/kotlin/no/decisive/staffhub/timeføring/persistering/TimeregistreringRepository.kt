package no.decisive.staffhub.timeføring.persistering

import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.timeføring.Timeregistrering
import org.springframework.stereotype.Repository

@Repository
class TimeregistreringRepository(
    private val timeregistreringTabell: TimeregistreringTabell
) {

    fun lagre(timeregistrering: Timeregistrering) {
        if (timeregistrering.erNy) {
            timeregistreringTabell.insert(tilRad(timeregistrering))
        }
        timeregistrering.bekreftPersistert()
    }

    fun hentPåId(id: Long): Timeregistrering {
        val rad = timeregistreringTabell.selectById(id)
            ?: throw IkkeFunnetException("Timeregistrering med id $id ble ikke funnet")
        return tilDomene(rad)
    }

    fun finnAlle(): List<Timeregistrering> {
        return timeregistreringTabell.selectAll().map { tilDomene(it) }
    }

    fun finnAlleForOppdrag(oppdragId: Long): List<Timeregistrering> {
        return timeregistreringTabell.selectByOppdragId(oppdragId).map { tilDomene(it) }
    }

    fun finnAlleForKonsulent(konsulentId: Long): List<Timeregistrering> {
        return timeregistreringTabell.selectByKonsulentId(konsulentId).map { tilDomene(it) }
    }

    fun finnAlleForOppdragOgKonsulent(oppdragId: Long, konsulentId: Long): List<Timeregistrering> {
        return timeregistreringTabell.selectByOppdragIdOgKonsulentId(oppdragId, konsulentId).map { tilDomene(it) }
    }

    private fun tilRad(timeregistrering: Timeregistrering) = TimeregistreringRad(
        id = timeregistrering.id,
        oppdragId = timeregistrering.oppdragId,
        konsulentId = timeregistrering.konsulentId,
        dato = timeregistrering.dato,
        timer = timeregistrering.timer,
        minutter = timeregistrering.minutter,
        beskrivelse = timeregistrering.beskrivelse,
        opprettetDato = timeregistrering.opprettetDato
    )

    private fun tilDomene(rad: TimeregistreringRad) = Timeregistrering.fra(
        Timeregistrering.PersistertState(
            id = rad.id,
            oppdragId = rad.oppdragId!!,
            konsulentId = rad.konsulentId!!,
            dato = rad.dato!!,
            timer = rad.timer!!,
            minutter = rad.minutter!!,
            beskrivelse = rad.beskrivelse!!,
            opprettetDato = rad.opprettetDato!!
        )
    )
}
