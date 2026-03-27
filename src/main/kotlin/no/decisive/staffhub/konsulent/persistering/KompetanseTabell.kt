package no.decisive.staffhub.konsulent.persistering

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

data class KompetanseRad(
    val id: Long,
    val konsulentId: Long,
    val fagområde: String?,
    val nivå: String?,
    val beskrivelse: String?
)

@Component
class KompetanseTabell(private val jdbc: JdbcTemplate) {

    private val radMapper = RowMapper<KompetanseRad> { rs, _ ->
        KompetanseRad(
            id = rs.getLong("id"),
            konsulentId = rs.getLong("konsulent_id"),
            fagområde = rs.getString("fagomrade"),
            nivå = rs.getString("niva"),
            beskrivelse = rs.getString("beskrivelse")
        )
    }

    fun insert(rad: KompetanseRad): KompetanseRad {
        jdbc.update(
            "INSERT INTO kompetanse (id, konsulent_id, fagomrade, niva, beskrivelse) VALUES (?, ?, ?, ?, ?)",
            rad.id, rad.konsulentId, rad.fagområde, rad.nivå, rad.beskrivelse
        )
        return rad
    }

    fun update(rad: KompetanseRad): KompetanseRad {
        jdbc.update(
            "UPDATE kompetanse SET fagomrade = ?, niva = ?, beskrivelse = ? WHERE id = ?",
            rad.fagområde, rad.nivå, rad.beskrivelse, rad.id
        )
        return rad
    }

    fun selectByKonsulentId(konsulentId: Long): List<KompetanseRad> {
        return jdbc.query("SELECT * FROM kompetanse WHERE konsulent_id = ?", radMapper, konsulentId)
    }

    fun selectAll(): List<KompetanseRad> {
        return jdbc.query("SELECT * FROM kompetanse", radMapper)
    }

    fun selectByFagområde(fagområde: String): List<KompetanseRad> {
        return jdbc.query("SELECT * FROM kompetanse WHERE fagomrade = ?", radMapper, fagområde)
    }

}
