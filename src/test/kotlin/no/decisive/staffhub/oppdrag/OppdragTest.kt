package no.decisive.staffhub.oppdrag

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.UgyldigStatusOvergangException
import no.decisive.staffhub.felles.ValideringException
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OppdragTest {

    private val idProvider = mock<IdProvider> {
        on { nesteOppdragId() }.thenReturn(1L)
    }

    private fun lagOppdrag(
        startDato: LocalDate = LocalDate.of(2026, 1, 1),
        sluttDato: LocalDate = LocalDate.of(2026, 6, 30),
        timepris: BigDecimal = BigDecimal("1500.00")
    ) = Oppdrag(
        idProvider = idProvider,
        tittel = "Modernisering",
        kundeNavn = "Acme AS",
        beskrivelse = "Modernisere backend",
        startDato = startDato,
        sluttDato = sluttDato,
        timepris = timepris,
        konsulentId = 1L
    )

    @Test
    fun `skal opprette oppdrag med idProvider`() {
        val oppdrag = lagOppdrag()

        assertThat(oppdrag.id).isEqualTo(1L)
        assertThat(oppdrag.tittel).isEqualTo("Modernisering")
        assertThat(oppdrag.kundeNavn).isEqualTo("Acme AS")
        assertThat(oppdrag.beskrivelse).isEqualTo("Modernisere backend")
        assertThat(oppdrag.startDato).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(oppdrag.sluttDato).isEqualTo(LocalDate.of(2026, 6, 30))
        assertThat(oppdrag.status).isEqualTo(Status.FORESLÅTT)
        assertThat(oppdrag.timepris).isEqualByComparingTo(BigDecimal("1500.00"))
        assertThat(oppdrag.konsulentId).isEqualTo(1L)
        assertThat(oppdrag.opprettetDato).isNotNull()
    }

    @Test
    fun `nytt oppdrag skal ha status FORESLÅTT`() {
        val oppdrag = lagOppdrag()

        assertThat(oppdrag.status).isEqualTo(Status.FORESLÅTT)
    }

    @Test
    fun `skal feile når sluttDato er før startDato`() {
        assertThatThrownBy {
            lagOppdrag(
                startDato = LocalDate.of(2026, 6, 30),
                sluttDato = LocalDate.of(2026, 1, 1)
            )
        }.isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("sluttDato")
    }

    @Test
    fun `skal feile når sluttDato er lik startDato`() {
        assertThatThrownBy {
            lagOppdrag(
                startDato = LocalDate.of(2026, 1, 1),
                sluttDato = LocalDate.of(2026, 1, 1)
            )
        }.isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("sluttDato")
    }

    @Test
    fun `skal feile når timepris er null eller negativ`() {
        assertThatThrownBy {
            lagOppdrag(timepris = BigDecimal.ZERO)
        }.isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("timepris")

        assertThatThrownBy {
            lagOppdrag(timepris = BigDecimal("-100"))
        }.isInstanceOf(ValideringException::class.java)
            .hasMessageContaining("timepris")
    }

    // Status-overganger

    @Test
    fun `skal endre status fra FORESLÅTT til BEKREFTET`() {
        val oppdrag = lagOppdrag()

        oppdrag.endreStatus(Status.BEKREFTET)

        assertThat(oppdrag.status).isEqualTo(Status.BEKREFTET)
    }

    @Test
    fun `skal endre status fra FORESLÅTT til KANSELLERT`() {
        val oppdrag = lagOppdrag()

        oppdrag.endreStatus(Status.KANSELLERT)

        assertThat(oppdrag.status).isEqualTo(Status.KANSELLERT)
    }

    @Test
    fun `skal endre status fra BEKREFTET til AKTIV`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)

        oppdrag.endreStatus(Status.AKTIV)

        assertThat(oppdrag.status).isEqualTo(Status.AKTIV)
    }

    @Test
    fun `skal endre status fra BEKREFTET til KANSELLERT`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)

        oppdrag.endreStatus(Status.KANSELLERT)

        assertThat(oppdrag.status).isEqualTo(Status.KANSELLERT)
    }

    @Test
    fun `skal endre status fra AKTIV til FULLFØRT`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)
        oppdrag.endreStatus(Status.AKTIV)

        oppdrag.endreStatus(Status.FULLFØRT)

        assertThat(oppdrag.status).isEqualTo(Status.FULLFØRT)
    }

    @Test
    fun `skal feile ved ugyldig overgang fra FORESLÅTT til AKTIV`() {
        val oppdrag = lagOppdrag()

        assertThatThrownBy { oppdrag.endreStatus(Status.AKTIV) }
            .isInstanceOf(UgyldigStatusOvergangException::class.java)
    }

    @Test
    fun `skal feile ved ugyldig overgang fra FORESLÅTT til FULLFØRT`() {
        val oppdrag = lagOppdrag()

        assertThatThrownBy { oppdrag.endreStatus(Status.FULLFØRT) }
            .isInstanceOf(UgyldigStatusOvergangException::class.java)
    }

    @Test
    fun `skal tillate overgang fra AKTIV til KANSELLERT`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)
        oppdrag.endreStatus(Status.AKTIV)

        oppdrag.endreStatus(Status.KANSELLERT)

        assertThat(oppdrag.status).isEqualTo(Status.KANSELLERT)
    }

    @Test
    fun `skal feile ved overgang fra FULLFØRT`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)
        oppdrag.endreStatus(Status.AKTIV)
        oppdrag.endreStatus(Status.FULLFØRT)

        assertThatThrownBy { oppdrag.endreStatus(Status.AKTIV) }
            .isInstanceOf(UgyldigStatusOvergangException::class.java)
    }

    @Test
    fun `skal feile ved overgang fra KANSELLERT`() {
        val oppdrag = lagOppdrag()
        oppdrag.endreStatus(Status.KANSELLERT)

        assertThatThrownBy { oppdrag.endreStatus(Status.FORESLÅTT) }
            .isInstanceOf(UgyldigStatusOvergangException::class.java)
    }

    // Overlapp

    @Test
    fun `skal detektere overlappende oppdrag`() {
        val oppdrag1 = lagOppdrag(
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 6, 30)
        )
        val oppdrag2 = lagOppdrag(
            startDato = LocalDate.of(2026, 3, 1),
            sluttDato = LocalDate.of(2026, 9, 30)
        )

        assertThat(oppdrag1.overlapper(oppdrag2)).isTrue()
        assertThat(oppdrag2.overlapper(oppdrag1)).isTrue()
    }

    @Test
    fun `skal detektere ikke-overlappende oppdrag`() {
        val oppdrag1 = lagOppdrag(
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 3, 31)
        )
        val oppdrag2 = lagOppdrag(
            startDato = LocalDate.of(2026, 4, 1),
            sluttDato = LocalDate.of(2026, 6, 30)
        )

        assertThat(oppdrag1.overlapper(oppdrag2)).isFalse()
        assertThat(oppdrag2.overlapper(oppdrag1)).isFalse()
    }

    @Test
    fun `skal detektere overlapp ved felles grensedato`() {
        val oppdrag1 = lagOppdrag(
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 3, 31)
        )
        val oppdrag2 = lagOppdrag(
            startDato = LocalDate.of(2026, 3, 31),
            sluttDato = LocalDate.of(2026, 6, 30)
        )

        assertThat(oppdrag1.overlapper(oppdrag2)).isTrue()
    }

    @Test
    fun `skal detektere overlapp når ett oppdrag inneholder et annet`() {
        val ytre = lagOppdrag(
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31)
        )
        val indre = lagOppdrag(
            startDato = LocalDate.of(2026, 3, 1),
            sluttDato = LocalDate.of(2026, 4, 30)
        )

        assertThat(ytre.overlapper(indre)).isTrue()
        assertThat(indre.overlapper(ytre)).isTrue()
    }

    // Dirty tracking

    @Test
    fun `nytt oppdrag skal være markert som nytt`() {
        val oppdrag = lagOppdrag()

        assertThat(oppdrag.erNy).isTrue()
        assertThat(oppdrag.erEndret).isFalse()
    }

    @Test
    fun `oppdrag skal ikke være nytt etter bekreftPersistert`() {
        val oppdrag = lagOppdrag()

        oppdrag.bekreftPersistert()

        assertThat(oppdrag.erNy).isFalse()
        assertThat(oppdrag.erEndret).isFalse()
    }

    @Test
    fun `oppdrag skal være markert som endret etter statusendring`() {
        val oppdrag = lagOppdrag()
        oppdrag.bekreftPersistert()

        oppdrag.endreStatus(Status.BEKREFTET)

        assertThat(oppdrag.erEndret).isTrue()
    }

    @Test
    fun `oppdrag skal være markert som endret etter feltendring`() {
        val oppdrag = lagOppdrag()
        oppdrag.bekreftPersistert()

        oppdrag.tittel = "Ny tittel"

        assertThat(oppdrag.erEndret).isTrue()
    }

    @Test
    fun `endrePeriode skal oppdatere datoer`() {
        val oppdrag = lagOppdrag()
        val nyStart = LocalDate.of(2026, 3, 1)
        val nySlutt = LocalDate.of(2026, 9, 30)

        oppdrag.endrePeriode(nyStart, nySlutt)

        assertThat(oppdrag.startDato).isEqualTo(nyStart)
        assertThat(oppdrag.sluttDato).isEqualTo(nySlutt)
    }

    @Test
    fun `endrePeriode skal kaste ValideringException ved ugyldig periode`() {
        val oppdrag = lagOppdrag()

        assertThatThrownBy {
            oppdrag.endrePeriode(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1))
        }.isInstanceOf(ValideringException::class.java)
    }

    @Test
    fun `endreTimepris skal oppdatere timepris`() {
        val oppdrag = lagOppdrag()

        oppdrag.endreTimepris(BigDecimal("2000.00"))

        assertThat(oppdrag.timepris).isEqualTo(BigDecimal("2000.00"))
    }

    @Test
    fun `endreTimepris skal kaste ValideringException ved negativ verdi`() {
        val oppdrag = lagOppdrag()

        assertThatThrownBy {
            oppdrag.endreTimepris(BigDecimal("-100"))
        }.isInstanceOf(ValideringException::class.java)
    }

    @Test
    fun `endreTimepris skal kaste ValideringException ved null-verdi`() {
        val oppdrag = lagOppdrag()

        assertThatThrownBy {
            oppdrag.endreTimepris(BigDecimal.ZERO)
        }.isInstanceOf(ValideringException::class.java)
    }

    @Test
    fun `oppdrag fra PersistertState skal ikke være nytt`() {
        val state = Oppdrag.PersistertState(
            id = 1L,
            tittel = "Modernisering",
            kundeNavn = "Acme AS",
            beskrivelse = null,
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 6, 30),
            status = Status.AKTIV,
            timepris = BigDecimal("1500.00"),
            konsulentId = 1L,
            opprettetDato = LocalDateTime.of(2026, 1, 1, 12, 0)
        )

        val oppdrag = Oppdrag.fra(state)

        assertThat(oppdrag.erNy).isFalse()
        assertThat(oppdrag.erEndret).isFalse()
        assertThat(oppdrag.status).isEqualTo(Status.AKTIV)
    }
}
