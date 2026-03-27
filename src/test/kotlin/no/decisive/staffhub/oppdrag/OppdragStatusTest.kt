package no.decisive.staffhub.oppdrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppdragStatusTest {

    @Test
    fun `FORESLÅTT kan gå til BEKREFTET og KANSELLERT`() {
        assertThat(OppdragStatus.FORESLÅTT.gyldigeOverganger())
            .containsExactlyInAnyOrder(OppdragStatus.BEKREFTET, OppdragStatus.KANSELLERT)
    }

    @Test
    fun `BEKREFTET kan gå til AKTIV og KANSELLERT`() {
        assertThat(OppdragStatus.BEKREFTET.gyldigeOverganger())
            .containsExactlyInAnyOrder(OppdragStatus.AKTIV, OppdragStatus.KANSELLERT)
    }

    @Test
    fun `AKTIV kan gå til FULLFØRT og KANSELLERT`() {
        assertThat(OppdragStatus.AKTIV.gyldigeOverganger())
            .containsExactlyInAnyOrder(OppdragStatus.FULLFØRT, OppdragStatus.KANSELLERT)
    }

    @Test
    fun `FULLFØRT har ingen gyldige overganger`() {
        assertThat(OppdragStatus.FULLFØRT.gyldigeOverganger()).isEmpty()
    }

    @Test
    fun `KANSELLERT har ingen gyldige overganger`() {
        assertThat(OppdragStatus.KANSELLERT.gyldigeOverganger()).isEmpty()
    }

    @Test
    fun `kanGåTil returnerer true for gyldige overganger`() {
        assertThat(OppdragStatus.FORESLÅTT.kanGåTil(OppdragStatus.BEKREFTET)).isTrue()
    }

    @Test
    fun `kanGåTil returnerer false for ugyldige overganger`() {
        assertThat(OppdragStatus.FORESLÅTT.kanGåTil(OppdragStatus.AKTIV)).isFalse()
    }
}
