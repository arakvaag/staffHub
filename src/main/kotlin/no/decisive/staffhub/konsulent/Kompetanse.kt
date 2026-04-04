package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider

class Kompetanse private constructor(
    val id: Long,
    val fagområde: Fagområde,
    val nivå: Kompetansenivå,
    val beskrivelse: String?,
) {
    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState

    constructor(
        idProvider: IdProvider,
        fagområde: Fagområde,
        nivå: Kompetansenivå,
        beskrivelse: String? = null,
    ) : this(
        id = idProvider.nesteKompetanseId(),
        fagområde = fagområde,
        nivå = nivå,
        beskrivelse = beskrivelse,
    )

    fun bekreftPersistert() {
        this.persistertState = tilState()
    }

    private fun tilState() = PersistertState(id, fagområde, nivå, beskrivelse)

    companion object {
        fun fra(state: PersistertState) = Kompetanse(
            id = state.id,
            fagområde = state.fagområde,
            nivå = state.nivå,
            beskrivelse = state.beskrivelse,
        ).apply {
            bekreftPersistert()
        }
    }

    enum class Kompetansenivå { JUNIOR, SENIOR, EKSPERT, }
    enum class Fagområde { BACKEND, FRONTEND, DEVOPS, ANALYSE, RÅDGIVNING, }

    data class PersistertState(
        val id: Long,
        val fagområde: Fagområde,
        val nivå: Kompetansenivå,
        val beskrivelse: String?,
    )
}
