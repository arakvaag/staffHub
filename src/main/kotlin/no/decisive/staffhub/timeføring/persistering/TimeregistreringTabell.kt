package no.decisive.staffhub.timeføring.persistering

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

data class TimeregistreringRad(
    val id: Long,
    val oppdragId: Long?,
    val konsulentId: Long?,
    val dato: LocalDate?,
    val timer: Int?,
    val minutter: Int?,
    val beskrivelse: String?,
    val opprettetDato: LocalDateTime?,
)

@Component
class TimeregistreringTabell(private val jdbc: JdbcTemplate) {

    private val radMapper = RowMapper { rs, _ ->
        TimeregistreringRad(
            id = rs.getLong("id"),
            oppdragId = rs.getObject("oppdrag_id") as? Long,
            konsulentId = rs.getObject("konsulent_id") as? Long,
            dato = rs.getDate("dato")?.toLocalDate(),
            timer = rs.getObject("timer") as? Int,
            minutter = rs.getObject("minutter") as? Int,
            beskrivelse = rs.getString("beskrivelse"),
            opprettetDato = rs.getTimestamp("opprettet_dato")?.toLocalDateTime()
        )
    }

    fun insert(rad: TimeregistreringRad): TimeregistreringRad {
        jdbc.update(
            "INSERT INTO timeregistrering (id, oppdrag_id, konsulent_id, dato, timer, minutter, beskrivelse, opprettet_dato) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            rad.id, rad.oppdragId, rad.konsulentId, rad.dato, rad.timer, rad.minutter, rad.beskrivelse, rad.opprettetDato
        )
        return rad
    }

    fun selectById(id: Long): TimeregistreringRad? {
        val resultat = jdbc.query("SELECT * FROM timeregistrering WHERE id = ?", radMapper, id)
        return resultat.firstOrNull()
    }

    fun selectAll(): List<TimeregistreringRad> {
        return jdbc.query("SELECT * FROM timeregistrering", radMapper)
    }

    fun selectByOppdragId(oppdragId: Long): List<TimeregistreringRad> {
        return jdbc.query("SELECT * FROM timeregistrering WHERE oppdrag_id = ?", radMapper, oppdragId)
    }

    fun selectByKonsulentId(konsulentId: Long): List<TimeregistreringRad> {
        return jdbc.query("SELECT * FROM timeregistrering WHERE konsulent_id = ?", radMapper, konsulentId)
    }

    fun selectByOppdragIdOgKonsulentId(oppdragId: Long, konsulentId: Long): List<TimeregistreringRad> {
        return jdbc.query(
            "SELECT * FROM timeregistrering WHERE oppdrag_id = ? AND konsulent_id = ?",
            radMapper, oppdragId, konsulentId
        )
    }
}
