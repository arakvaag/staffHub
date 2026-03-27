package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider
import java.time.LocalDateTime

class Konsulent private constructor(
    val id: Long,
    var fornavn: String,
    var etternavn: String,
    var epost: String,
    var telefon: String?,
    val opprettetDato: LocalDateTime,
    kompetanser: List<Kompetanse>
) {
    private val _kompetanser: MutableList<Kompetanse> = kompetanser.toMutableList()
    val kompetanser: List<Kompetanse> get() = _kompetanser.toList()

    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState

    constructor(
        idProvider: IdProvider,
        fornavn: String,
        etternavn: String,
        epost: String,
        telefon: String? = null,
        kompetanser: List<Kompetanse> = emptyList()
    ) : this(
        id = idProvider.nesteKonsulentId(),
        fornavn = fornavn,
        etternavn = etternavn,
        epost = epost,
        telefon = telefon,
        opprettetDato = LocalDateTime.now(),
        kompetanser = kompetanser
    )

    fun leggTilKompetanse(kompetanse: Kompetanse) {
        _kompetanser.add(kompetanse)
    }

    fun bekreftPersistert() {
        this.persistertState = tilState()
        _kompetanser.forEach { it.bekreftPersistert() }
    }

    private fun tilState() = PersistertState(
        id, fornavn, etternavn, epost, telefon, opprettetDato,
        _kompetanser.map { Kompetanse.PersistertState(it.id, it.fagområde, it.nivå, it.beskrivelse) }
    )

    data class PersistertState(
        val id: Long,
        val fornavn: String,
        val etternavn: String,
        val epost: String,
        val telefon: String?,
        val opprettetDato: LocalDateTime,
        val kompetanser: List<Kompetanse.PersistertState>
    )

    companion object {
        fun fra(state: PersistertState): Konsulent {
            val kompetanser = state.kompetanser.map { Kompetanse.fra(it) }
            val konsulent = Konsulent(
                id = state.id,
                fornavn = state.fornavn,
                etternavn = state.etternavn,
                epost = state.epost,
                telefon = state.telefon,
                opprettetDato = state.opprettetDato,
                kompetanser = kompetanser
            )
            konsulent.bekreftPersistert()
            return konsulent
        }
    }
}
