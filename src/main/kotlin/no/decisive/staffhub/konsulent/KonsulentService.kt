package no.decisive.staffhub.konsulent

import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KonsulentService(
    private val konsulentRepository: KonsulentRepository
) {

    fun opprett(konsulent: Konsulent): Konsulent {
        konsulentRepository.lagre(konsulent)
        return konsulent
    }

    @Transactional(readOnly = true)
    fun hentAlle(fagområde: Fagområde? = null): List<Konsulent> =
        if (fagområde != null) {
            konsulentRepository.finnAlleMedFagområde(fagområde)
        } else {
            konsulentRepository.finnAlle()
        }

}
