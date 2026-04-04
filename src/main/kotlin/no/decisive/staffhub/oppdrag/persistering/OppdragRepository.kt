package no.decisive.staffhub.oppdrag.persistering

import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.oppdrag.Oppdrag
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import org.springframework.stereotype.Repository

@Repository
class OppdragRepository(
    private val oppdragTabell: OppdragTabell
) {

    fun lagre(oppdrag: Oppdrag) {
        if (oppdrag.erNy) {
            oppdragTabell.insert(tilRad(oppdrag))
        } else if (oppdrag.erEndret) {
            oppdragTabell.update(tilRad(oppdrag))
        }
        oppdrag.bekreftPersistert()
    }

    fun hentPåId(id: Long): Oppdrag {
        val rad = oppdragTabell.selectById(id)
            ?: throw IkkeFunnetException("Oppdrag med id $id ble ikke funnet")
        return tilDomene(rad)
    }

    fun finnAlle(): List<Oppdrag> {
        return oppdragTabell.selectAll().map { tilDomene(it) }
    }

    fun finnAktiveForKonsulent(konsulentId: Long): List<Oppdrag> {
        return oppdragTabell.selectByKonsulentIdOgStatus(konsulentId, Status.AKTIV.name)
            .map { tilDomene(it) }
    }

    private fun tilRad(oppdrag: Oppdrag) = OppdragRad(
        id = oppdrag.id,
        tittel = oppdrag.tittel,
        kundeNavn = oppdrag.kundeNavn,
        beskrivelse = oppdrag.beskrivelse,
        startDato = oppdrag.startDato,
        sluttDato = oppdrag.sluttDato,
        status = oppdrag.status.name,
        timepris = oppdrag.timepris,
        konsulentId = oppdrag.konsulentId,
        opprettetDato = oppdrag.opprettetDato
    )

    private fun tilDomene(rad: OppdragRad) = Oppdrag.fra(
        Oppdrag.PersistertState(
            id = rad.id,
            tittel = rad.tittel!!,
            kundeNavn = rad.kundeNavn!!,
            beskrivelse = rad.beskrivelse,
            startDato = rad.startDato!!,
            sluttDato = rad.sluttDato!!,
            status = Status.valueOf(rad.status!!),
            timepris = rad.timepris!!,
            konsulentId = rad.konsulentId!!,
            opprettetDato = rad.opprettetDato!!
        )
    )
}
