package no.decisive.staffhub.oppdrag

import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.felles.OverlappException
import no.decisive.staffhub.felles.ValideringException
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OppdragService(
    private val oppdragRepository: OppdragRepository,
    private val konsulentRepository: KonsulentRepository
) {

    fun opprett(oppdrag: Oppdrag): Oppdrag {
        try {
            konsulentRepository.hentPåId(oppdrag.konsulentId)
        } catch (_: IkkeFunnetException) {
            throw ValideringException("Konsulent med id ${oppdrag.konsulentId} finnes ikke")
        }
        oppdragRepository.lagre(oppdrag)
        return oppdrag
    }

    fun endreStatus(id: Long, nyStatus: OppdragStatus): Oppdrag {
        val oppdrag = oppdragRepository.hentPåId(id)
        oppdrag.endreStatus(nyStatus)

        if (nyStatus == OppdragStatus.AKTIV) {
            sjekkOverlapp(oppdrag)
        }

        oppdragRepository.lagre(oppdrag)
        return oppdrag
    }

    private fun sjekkOverlapp(oppdrag: Oppdrag) {
        val aktiveOppdrag = oppdragRepository.finnAktiveForKonsulent(oppdrag.konsulentId)
        val overlappende = aktiveOppdrag.filter { it.id != oppdrag.id && it.overlapper(oppdrag) }
        if (overlappende.isNotEmpty()) {
            throw OverlappException(
                "Konsulenten har allerede et aktivt oppdrag som overlapper med denne perioden"
            )
        }
    }
}
