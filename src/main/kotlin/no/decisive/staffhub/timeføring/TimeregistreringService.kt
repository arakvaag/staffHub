package no.decisive.staffhub.timeføring

import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.felles.ValideringException
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
import no.decisive.staffhub.timeføring.persistering.TimeregistreringRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TimeregistreringService(
    private val timeregistreringRepository: TimeregistreringRepository,
    private val oppdragRepository: OppdragRepository,
    private val konsulentRepository: KonsulentRepository,
) {

    fun opprett(timeregistrering: Timeregistrering): Timeregistrering {
        val oppdrag = try {
            oppdragRepository.hentPåId(timeregistrering.oppdragId)
        } catch (_: IkkeFunnetException) {
            throw ValideringException("Oppdrag med id ${timeregistrering.oppdragId} finnes ikke")
        }

        if (oppdrag.status != Status.AKTIV) {
            throw ValideringException("Oppdrag med id ${timeregistrering.oppdragId} er ikke aktivt")
        }

        try {
            konsulentRepository.hentPåId(timeregistrering.konsulentId)
        } catch (_: IkkeFunnetException) {
            throw ValideringException("Konsulent med id ${timeregistrering.konsulentId} finnes ikke")
        }

        if (timeregistrering.konsulentId != oppdrag.konsulentId) {
            throw ValideringException("Konsulent med id ${timeregistrering.konsulentId} er ikke tilknyttet oppdrag med id ${timeregistrering.oppdragId}")
        }

        timeregistreringRepository.lagre(timeregistrering)
        return timeregistrering
    }

    @Transactional(readOnly = true)
    fun hentPåId(id: Long): Timeregistrering {
        return timeregistreringRepository.hentPåId(id)
    }

    @Transactional(readOnly = true)
    fun finnAlle(): List<Timeregistrering> {
        return timeregistreringRepository.finnAlle()
    }

    @Transactional(readOnly = true)
    fun finnAlleForOppdrag(oppdragId: Long): List<Timeregistrering> {
        return timeregistreringRepository.finnAlleForOppdrag(oppdragId)
    }

    @Transactional(readOnly = true)
    fun finnAlleForKonsulent(konsulentId: Long): List<Timeregistrering> {
        return timeregistreringRepository.finnAlleForKonsulent(konsulentId)
    }

    @Transactional(readOnly = true)
    fun finnAlleForOppdragOgKonsulent(oppdragId: Long, konsulentId: Long): List<Timeregistrering> {
        return timeregistreringRepository.finnAlleForOppdragOgKonsulent(oppdragId, konsulentId)
    }
}
