package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider
import java.time.LocalDateTime

class Konsulent private constructor(
    val id: Long,
    fornavn: String,
    etternavn: String,
    epost: String,
    telefon: String?,
    val opprettetDato: LocalDateTime,
    kompetanser: List<Kompetanse>,
    versjon: Int,
) {
    private val _kompetanser: MutableList<Kompetanse> = kompetanser.toMutableList()
    val kompetanser: List<Kompetanse> get() = _kompetanser.toList()

    var fornavn: String = fornavn
        set(value) { field = value; versjon++ }
    var etternavn: String = etternavn
        set(value) { field = value; versjon++ }
    var epost: String = epost
        set(value) { field = value; versjon++ }
    var telefon: String? = telefon
        set(value) { field = value; versjon++ }
    var versjon: Int = versjon
        private set

    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState
    val persistertVersjon: Int? get() = persistertState?.versjon

    constructor(
        idProvider: IdProvider,
        fornavn: String,
        etternavn: String,
        epost: String,
        telefon: String? = null,
        kompetanser: List<Kompetanse> = emptyList(),
    ) : this(
        id = idProvider.nesteKonsulentId(),
        fornavn = fornavn,
        etternavn = etternavn,
        epost = epost,
        telefon = telefon,
        opprettetDato = LocalDateTime.now(),
        kompetanser = kompetanser,
        versjon = 1,
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
        _kompetanser.map { Kompetanse.PersistertState(it.id, it.fagområde, it.nivå, it.beskrivelse) },
        versjon,
    )

    companion object {
        fun fra(state: PersistertState): Konsulent {
            val kompetanser = state.kompetanser.map { Kompetanse.fra(it) }
            return Konsulent(
                id = state.id,
                fornavn = state.fornavn,
                etternavn = state.etternavn,
                epost = state.epost,
                telefon = state.telefon,
                opprettetDato = state.opprettetDato,
                kompetanser = kompetanser,
                versjon = state.versjon,
            ).apply {
                bekreftPersistert()
            }
        }
    }

    data class PersistertState(
        val id: Long,
        val fornavn: String,
        val etternavn: String,
        val epost: String,
        val telefon: String?,
        val opprettetDato: LocalDateTime,
        val kompetanser: List<Kompetanse.PersistertState>,
        val versjon: Int,
    )
}
