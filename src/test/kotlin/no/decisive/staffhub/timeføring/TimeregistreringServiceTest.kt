package no.decisive.staffhub.timeføring

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.felles.ValideringException
import no.decisive.staffhub.konsulent.Konsulent
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.oppdrag.Oppdrag
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
import no.decisive.staffhub.timeføring.persistering.TimeregistreringRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class TimeregistreringServiceTest {

    private val timeregistreringRepository = mock<TimeregistreringRepository>()
    private val oppdragRepository = mock<OppdragRepository>()
    private val konsulentRepository = mock<KonsulentRepository>()
    private val service = TimeregistreringService(timeregistreringRepository, oppdragRepository, konsulentRepository)

    @Test
    fun `skal opprette timeregistrering`() {
        // given
        val oppdrag = lagTestOppdrag()
        val konsulent = lagTestKonsulent()
        val timeregistrering = lagTestTimeregistrering()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)
        whenever(konsulentRepository.hentPåId(1L)).thenReturn(konsulent)

        // when
        val resultat = service.opprett(timeregistrering)

        // then
        assertThat(resultat.timer).isEqualTo(7)
        assertThat(resultat.minutter).isEqualTo(30)
        verify(timeregistreringRepository).lagre(any())
    }

    @Test
    fun `skal feile når oppdrag ikke finnes`() {
        val timeregistrering = lagTestTimeregistrering()
        whenever(oppdragRepository.hentPåId(1L)).thenThrow(IkkeFunnetException("Oppdrag med id 1 ikke funnet"))

        assertThatThrownBy { service.opprett(timeregistrering) }
            .isInstanceOf(ValideringException::class.java)
        verify(timeregistreringRepository, never()).lagre(any())
    }

    @Test
    fun `skal feile når konsulent ikke finnes`() {
        val oppdrag = lagTestOppdrag()
        val timeregistrering = lagTestTimeregistrering()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)
        whenever(konsulentRepository.hentPåId(1L)).thenThrow(IkkeFunnetException("Konsulent med id 1 ikke funnet"))

        assertThatThrownBy { service.opprett(timeregistrering) }
            .isInstanceOf(ValideringException::class.java)
        verify(timeregistreringRepository, never()).lagre(any())
    }

    @Test
    fun `skal feile når konsulentId ikke matcher oppdragets konsulentId`() {
        val oppdrag = lagTestOppdrag(konsulentId = 2L)
        val konsulent = lagTestKonsulent()
        val timeregistrering = lagTestTimeregistrering()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)
        whenever(konsulentRepository.hentPåId(1L)).thenReturn(konsulent)

        assertThatThrownBy { service.opprett(timeregistrering) }
            .isInstanceOf(ValideringException::class.java)
        verify(timeregistreringRepository, never()).lagre(any())
    }

    @Test
    fun `skal feile når oppdrag ikke er AKTIV`() {
        val oppdrag = lagTestOppdrag(status = Status.FORESLÅTT)
        val timeregistrering = lagTestTimeregistrering()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)

        assertThatThrownBy { service.opprett(timeregistrering) }
            .isInstanceOf(ValideringException::class.java)
        verify(timeregistreringRepository, never()).lagre(any())
    }

    private fun lagTestTimeregistrering(): Timeregistrering {
        val mockIdProvider = mock<IdProvider> {
            on { nesteTimeregistreringId() }.thenReturn(1L)
        }
        return Timeregistrering(
            idProvider = mockIdProvider,
            oppdragId = 1L,
            konsulentId = 1L,
            dato = LocalDate.of(2026, 6, 15),
            timer = 7,
            minutter = 30,
            beskrivelse = "Backend-utvikling"
        )
    }

    private fun lagTestOppdrag(
        konsulentId: Long = 1L,
        status: Status = Status.AKTIV,
    ): Oppdrag {
        val mockIdProvider = mock<IdProvider> {
            on { nesteOppdragId() }.thenReturn(1L)
        }
        val oppdrag = Oppdrag(
            idProvider = mockIdProvider,
            tittel = "Modernisering",
            kundeNavn = "Acme AS",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31),
            timepris = BigDecimal("1200.00"),
            konsulentId = konsulentId
        )
        when (status) {
            Status.BEKREFTET -> oppdrag.endreStatus(Status.BEKREFTET)
            Status.AKTIV -> {
                oppdrag.endreStatus(Status.BEKREFTET)
                oppdrag.endreStatus(Status.AKTIV)
            }
            Status.FULLFØRT -> {
                oppdrag.endreStatus(Status.BEKREFTET)
                oppdrag.endreStatus(Status.AKTIV)
                oppdrag.endreStatus(Status.FULLFØRT)
            }
            else -> {} // FORESLÅTT er default, KANSELLERT ikke testet
        }
        return oppdrag
    }

    private fun lagTestKonsulent(): Konsulent {
        val mockIdProvider = mock<IdProvider> {
            on { nesteKonsulentId() }.thenReturn(1L)
        }
        return Konsulent(
            idProvider = mockIdProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
    }
}
