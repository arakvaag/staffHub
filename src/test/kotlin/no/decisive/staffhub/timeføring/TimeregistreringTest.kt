package no.decisive.staffhub.timeføring

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.ValideringException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDate
import java.time.LocalDateTime

class TimeregistreringTest {

    private val idProvider = mock<IdProvider> {
        on { nesteTimeregistreringId() }.thenReturn(1L)
    }

    private fun lagTimeregistrering(
        timer: Int = 7,
        minutter: Int = 30
    ) = Timeregistrering(
        idProvider = idProvider,
        oppdragId = 1L,
        konsulentId = 1L,
        dato = LocalDate.of(2026, 3, 15),
        timer = timer,
        minutter = minutter,
        beskrivelse = "Utvikling"
    )

    @Test
    fun `skal opprette timeregistrering`() {
        val reg = Timeregistrering(
            idProvider = idProvider,
            oppdragId = 1L,
            konsulentId = 1L,
            dato = LocalDate.of(2026, 3, 15),
            timer = 7,
            minutter = 30,
            beskrivelse = "Utvikling"
        )

        assertThat(reg.id).isEqualTo(1L)
        assertThat(reg.oppdragId).isEqualTo(1L)
        assertThat(reg.konsulentId).isEqualTo(1L)
        assertThat(reg.dato).isEqualTo(LocalDate.of(2026, 3, 15))
        assertThat(reg.timer).isEqualTo(7)
        assertThat(reg.minutter).isEqualTo(30)
        assertThat(reg.beskrivelse).isEqualTo("Utvikling")
        assertThat(reg.opprettetDato).isNotNull()
    }

    @Test
    fun `totalMinutter skal beregnes korrekt`() {
        val reg = lagTimeregistrering(timer = 7, minutter = 30)

        assertThat(reg.totalMinutter).isEqualTo(450)
    }

    @Test
    fun `totalMinutter med bare timer`() {
        val reg = lagTimeregistrering(timer = 8, minutter = 0)

        assertThat(reg.totalMinutter).isEqualTo(480)
    }

    @Test
    fun `totalMinutter med bare minutter`() {
        val reg = lagTimeregistrering(timer = 0, minutter = 45)

        assertThat(reg.totalMinutter).isEqualTo(45)
    }

    // Validering

    @Test
    fun `skal feile ved negative timer`() {
        assertThatThrownBy { lagTimeregistrering(timer = -1) }
            .isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("timer")
    }

    @Test
    fun `skal feile ved timer over 23`() {
        assertThatThrownBy { lagTimeregistrering(timer = 24) }
            .isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("timer")
    }

    @Test
    fun `skal feile ved negative minutter`() {
        assertThatThrownBy { lagTimeregistrering(minutter = -1) }
            .isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("minutter")
    }

    @Test
    fun `skal feile ved minutter over 59`() {
        assertThatThrownBy { lagTimeregistrering(minutter = 60) }
            .isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("minutter")
    }

    @Test
    fun `skal feile ved 0 timer og 0 minutter`() {
        assertThatThrownBy { lagTimeregistrering(timer = 0, minutter = 0) }
            .isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("minst 1 minutt")
    }

    @Test
    fun `skal tillate grenseverdier`() {
        val minReg = lagTimeregistrering(timer = 0, minutter = 1)
        assertThat(minReg.totalMinutter).isEqualTo(1)

        val maxReg = lagTimeregistrering(timer = 23, minutter = 59)
        assertThat(maxReg.totalMinutter).isEqualTo(1439)
    }

    // Dirty tracking

    @Test
    fun `ny timeregistrering skal være markert som ny`() {
        val reg = lagTimeregistrering()

        assertThat(reg.erNy).isTrue()
        assertThat(reg.erEndret).isFalse()
    }

    @Test
    fun `timeregistrering skal ikke være ny etter bekreftPersistert`() {
        val reg = lagTimeregistrering()

        reg.bekreftPersistert()

        assertThat(reg.erNy).isFalse()
        assertThat(reg.erEndret).isFalse()
    }

    @Test
    fun `timeregistrering fra PersistertState skal ikke være ny`() {
        val state = Timeregistrering.PersistertState(
            id = 1L,
            oppdragId = 1L,
            konsulentId = 1L,
            dato = LocalDate.of(2026, 3, 15),
            timer = 7,
            minutter = 30,
            beskrivelse = "Utvikling",
            opprettetDato = LocalDateTime.of(2026, 3, 15, 16, 0)
        )

        val reg = Timeregistrering.fra(state)

        assertThat(reg.erNy).isFalse()
        assertThat(reg.erEndret).isFalse()
        assertThat(reg.id).isEqualTo(1L)
        assertThat(reg.timer).isEqualTo(7)
        assertThat(reg.minutter).isEqualTo(30)
    }
}
