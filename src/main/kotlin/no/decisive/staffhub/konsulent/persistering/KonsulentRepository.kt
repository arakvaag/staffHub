package no.decisive.staffhub.konsulent.persistering

import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.felles.OptimistiskLåsingException
import no.decisive.staffhub.konsulent.Kompetanse
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import no.decisive.staffhub.konsulent.Konsulent
import org.springframework.stereotype.Repository

@Repository
class KonsulentRepository(
    private val konsulentTabell: KonsulentTabell,
    private val kompetanseTabell: KompetanseTabell
) {

    fun lagre(konsulent: Konsulent) {
        if (konsulent.erNy) {
            konsulentTabell.insert(tilRad(konsulent))
        } else if (konsulent.erEndret) {
            val raderOppdatert = konsulentTabell.update(tilRad(konsulent), konsulent.persistertVersjon!!)
            if (raderOppdatert == 0) {
                throw OptimistiskLåsingException("Konsulent med id ${konsulent.id} ble endret av en annen transaksjon")
            }
        }

        for (kompetanse in konsulent.kompetanser) {
            if (kompetanse.erNy) {
                kompetanseTabell.insert(tilKompetanseRad(kompetanse, konsulent.id))
            } else if (kompetanse.erEndret) {
                kompetanseTabell.update(tilKompetanseRad(kompetanse, konsulent.id))
            }
        }

        konsulent.bekreftPersistert()
    }

    fun hentPåId(id: Long): Konsulent {
        val rad = konsulentTabell.selectById(id)
            ?: throw IkkeFunnetException("Konsulent med id $id ble ikke funnet")
        val kompetanseRader = kompetanseTabell.selectByKonsulentId(id)
        return tilDomene(rad, kompetanseRader)
    }

    fun finnAlle(): List<Konsulent> {
        val rader = konsulentTabell.selectAll()
        val kompetanserPerKonsulent = kompetanseTabell.selectAll().groupBy { it.konsulentId }
        return rader.map { rad ->
            tilDomene(rad, kompetanserPerKonsulent[rad.id] ?: emptyList())
        }
    }

    fun finnAlleMedFagområde(fagområde: Fagområde): List<Konsulent> {
        val alleKompetanser = kompetanseTabell.selectAll()
        val konsulentIder = alleKompetanser
            .filter { it.fagområde == fagområde.name }
            .map { it.konsulentId }
            .distinct()
            .toSet()
        if (konsulentIder.isEmpty()) return emptyList()
        val kompetanserPerKonsulent = alleKompetanser.groupBy { it.konsulentId }
        val rader = konsulentTabell.selectAll().filter { it.id in konsulentIder }
        return rader.map { rad ->
            tilDomene(rad, kompetanserPerKonsulent[rad.id] ?: emptyList())
        }
    }

    private fun tilRad(konsulent: Konsulent) = KonsulentRad(
        id = konsulent.id,
        fornavn = konsulent.fornavn,
        etternavn = konsulent.etternavn,
        epost = konsulent.epost,
        telefon = konsulent.telefon,
        opprettetDato = konsulent.opprettetDato,
        versjon = konsulent.versjon
    )

    private fun tilKompetanseRad(kompetanse: Kompetanse, konsulentId: Long) = KompetanseRad(
        id = kompetanse.id,
        konsulentId = konsulentId,
        fagområde = kompetanse.fagområde.name,
        nivå = kompetanse.nivå.name,
        beskrivelse = kompetanse.beskrivelse
    )

    private fun tilDomene(rad: KonsulentRad, kompetanseRader: List<KompetanseRad>): Konsulent {
        val kompetanser = kompetanseRader.map { tilKompetanseState(it) }
        return Konsulent.fra(
            Konsulent.PersistertState(
                id = rad.id,
                fornavn = rad.fornavn!!,
                etternavn = rad.etternavn!!,
                epost = rad.epost!!,
                telefon = rad.telefon,
                opprettetDato = rad.opprettetDato!!,
                kompetanser = kompetanser,
                versjon = rad.versjon!!
            )
        )
    }

    private fun tilKompetanseState(rad: KompetanseRad) = Kompetanse.PersistertState(
        id = rad.id,
        fagområde = Fagområde.valueOf(rad.fagområde!!),
        nivå = Kompetansenivå.valueOf(rad.nivå!!),
        beskrivelse = rad.beskrivelse
    )
}
