package no.decisive.staffhub.oppdrag.persistering

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class OppdragRad(
    val id: Long,
    val tittel: String?,
    val kundeNavn: String?,
    val beskrivelse: String?,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: String?,
    val timepris: BigDecimal?,
    val konsulentId: Long?,
    val opprettetDato: LocalDateTime?
)

@Component
class OppdragTabell(private val jdbc: JdbcTemplate) {

    private val radMapper = RowMapper { rs, _ ->
        OppdragRad(
            id = rs.getLong("id"),
            tittel = rs.getString("tittel"),
            kundeNavn = rs.getString("kunde_navn"),
            beskrivelse = rs.getString("beskrivelse"),
            startDato = rs.getDate("start_dato")?.toLocalDate(),
            sluttDato = rs.getDate("slutt_dato")?.toLocalDate(),
            status = rs.getString("status"),
            timepris = rs.getBigDecimal("timepris"),
            konsulentId = rs.getLong("konsulent_id"),
            opprettetDato = rs.getTimestamp("opprettet_dato")?.toLocalDateTime()
        )
    }

    fun insert(rad: OppdragRad): OppdragRad {
        jdbc.update(
            """INSERT INTO oppdrag (id, tittel, kunde_navn, beskrivelse, start_dato, slutt_dato, status, timepris, konsulent_id, opprettet_dato)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            rad.id, rad.tittel, rad.kundeNavn, rad.beskrivelse,
            rad.startDato, rad.sluttDato, rad.status, rad.timepris,
            rad.konsulentId, rad.opprettetDato
        )
        return rad
    }

    fun update(rad: OppdragRad): OppdragRad {
        jdbc.update(
            """UPDATE oppdrag SET tittel = ?, kunde_navn = ?, beskrivelse = ?, start_dato = ?, slutt_dato = ?,
               status = ?, timepris = ? WHERE id = ?""",
            rad.tittel, rad.kundeNavn, rad.beskrivelse, rad.startDato, rad.sluttDato,
            rad.status, rad.timepris, rad.id
        )
        return rad
    }

    fun selectById(id: Long): OppdragRad? {
        val resultat = jdbc.query("SELECT * FROM oppdrag WHERE id = ?", radMapper, id)
        return resultat.firstOrNull()
    }

    fun selectAll(): List<OppdragRad> {
        return jdbc.query("SELECT * FROM oppdrag", radMapper)
    }

    fun selectByKonsulentIdOgStatus(konsulentId: Long, status: String): List<OppdragRad> {
        return jdbc.query(
            "SELECT * FROM oppdrag WHERE konsulent_id = ? AND status = ?",
            radMapper, konsulentId, status
        )
    }
}
