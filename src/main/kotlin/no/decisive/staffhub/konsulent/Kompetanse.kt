package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider

class Kompetanse private constructor(
    val id: Long,
    val fagområde: Fagområde,
    val nivå: Kompetansenivå,
    val beskrivelse: String?
) {
    enum class Kompetansenivå { JUNIOR, SENIOR, EKSPERT }
    enum class Fagområde { BACKEND, FRONTEND, DEVOPS, ANALYSE, RÅDGIVNING }

    private var persistertState: PersistertState? = null

    val erNy: Boolean get() = persistertState == null
    val erEndret: Boolean get() = persistertState != null && tilState() != persistertState

    constructor(
        idProvider: IdProvider,
        fagområde: Fagområde,
        nivå: Kompetansenivå,
        beskrivelse: String? = null
    ) : this(
        id = idProvider.nesteKompetanseId(),
        fagområde = fagområde,
        nivå = nivå,
        beskrivelse = beskrivelse
    )

    fun bekreftPersistert() {
        this.persistertState = tilState()
    }

    private fun tilState() = PersistertState(id, fagområde, nivå, beskrivelse)

    data class PersistertState(
        val id: Long,
        val fagområde: Fagområde,
        val nivå: Kompetansenivå,
        val beskrivelse: String?
    )

    companion object {
        fun fra(state: PersistertState): Kompetanse {
            val kompetanse = Kompetanse(
                id = state.id,
                fagområde = state.fagområde,
                nivå = state.nivå,
                beskrivelse = state.beskrivelse
            )
            kompetanse.bekreftPersistert()
            return kompetanse
        }
    }
}
