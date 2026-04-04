package no.decisive.staffhub.konsulent.persistering

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class KonsulentRad(
    val id: Long,
    val fornavn: String?,
    val etternavn: String?,
    val epost: String?,
    val telefon: String?,
    val opprettetDato: LocalDateTime?,
    val versjon: Int?,
)

@Component
class KonsulentTabell(private val jdbc: JdbcTemplate) {

    private val radMapper = RowMapper { rs, _ ->
        KonsulentRad(
            id = rs.getLong("id"),
            fornavn = rs.getString("fornavn"),
            etternavn = rs.getString("etternavn"),
            epost = rs.getString("epost"),
            telefon = rs.getString("telefon"),
            opprettetDato = rs.getTimestamp("opprettet_dato")?.toLocalDateTime(),
            versjon = rs.getObject("versjon") as? Int
        )
    }

    fun insert(rad: KonsulentRad): KonsulentRad {
        jdbc.update(
            "INSERT INTO konsulent (id, fornavn, etternavn, epost, telefon, opprettet_dato, versjon) VALUES (?, ?, ?, ?, ?, ?, ?)",
            rad.id, rad.fornavn, rad.etternavn, rad.epost, rad.telefon, rad.opprettetDato, rad.versjon
        )
        return rad
    }

    fun update(rad: KonsulentRad, forventetVersjon: Int): Int {
        return jdbc.update(
            "UPDATE konsulent SET fornavn = ?, etternavn = ?, epost = ?, telefon = ?, versjon = ? WHERE id = ? AND versjon = ?",
            rad.fornavn, rad.etternavn, rad.epost, rad.telefon, rad.versjon, rad.id, forventetVersjon
        )
    }

    fun selectById(id: Long): KonsulentRad? {
        val resultat = jdbc.query("SELECT * FROM konsulent WHERE id = ?", radMapper, id)
        return resultat.firstOrNull()
    }

    fun selectAll(): List<KonsulentRad> {
        return jdbc.query("SELECT * FROM konsulent", radMapper)
    }
}
