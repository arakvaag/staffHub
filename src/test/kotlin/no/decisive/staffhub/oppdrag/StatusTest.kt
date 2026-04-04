package no.decisive.staffhub.oppdrag

import no.decisive.staffhub.oppdrag.Oppdrag.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatusTest {

    @Test
    fun `FORESLÅTT kan gå til BEKREFTET og KANSELLERT`() {
        assertThat(Status.FORESLÅTT.gyldigeOverganger())
            .containsExactlyInAnyOrder(Status.BEKREFTET, Status.KANSELLERT)
    }

    @Test
    fun `BEKREFTET kan gå til AKTIV og KANSELLERT`() {
        assertThat(Status.BEKREFTET.gyldigeOverganger())
            .containsExactlyInAnyOrder(Status.AKTIV, Status.KANSELLERT)
    }

    @Test
    fun `AKTIV kan gå til FULLFØRT og KANSELLERT`() {
        assertThat(Status.AKTIV.gyldigeOverganger())
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.KANSELLERT)
    }

    @Test
    fun `FULLFØRT har ingen gyldige overganger`() {
        assertThat(Status.FULLFØRT.gyldigeOverganger()).isEmpty()
    }

    @Test
    fun `KANSELLERT har ingen gyldige overganger`() {
        assertThat(Status.KANSELLERT.gyldigeOverganger()).isEmpty()
    }

    @Test
    fun `kanGåTil returnerer true for gyldige overganger`() {
        assertThat(Status.FORESLÅTT.kanGåTil(Status.BEKREFTET)).isTrue()
    }

    @Test
    fun `kanGåTil returnerer false for ugyldige overganger`() {
        assertThat(Status.FORESLÅTT.kanGåTil(Status.AKTIV)).isFalse()
    }
}
