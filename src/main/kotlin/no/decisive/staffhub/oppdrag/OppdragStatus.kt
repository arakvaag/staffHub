package no.decisive.staffhub.oppdrag

enum class OppdragStatus {
    FORESLÅTT,
    BEKREFTET,
    AKTIV,
    FULLFØRT,
    KANSELLERT;

    fun gyldigeOverganger(): Set<OppdragStatus> = when (this) {
        FORESLÅTT -> setOf(BEKREFTET, KANSELLERT)
        BEKREFTET -> setOf(AKTIV, KANSELLERT)
        AKTIV -> setOf(FULLFØRT, KANSELLERT)
        FULLFØRT -> emptySet()
        KANSELLERT -> emptySet()
    }

    fun kanGåTil(nyStatus: OppdragStatus): Boolean = nyStatus in gyldigeOverganger()
}
