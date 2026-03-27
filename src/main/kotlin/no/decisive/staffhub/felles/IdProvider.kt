package no.decisive.staffhub.felles

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component

@Component
class IdProvider(private val jdbc: JdbcTemplate) {

    private fun nesteId(sekvensnavn: String): Long =
        jdbc.queryForObject<Long>("SELECT nextval('$sekvensnavn')")!!

    fun nesteKonsulentId(): Long = nesteId("konsulent_id_seq")
    fun nesteKompetanseId(): Long = nesteId("kompetanse_id_seq")
    fun nesteOppdragId(): Long = nesteId("oppdrag_id_seq")
    fun nesteTimeregistreringId(): Long = nesteId("timeregistrering_id_seq")
}
