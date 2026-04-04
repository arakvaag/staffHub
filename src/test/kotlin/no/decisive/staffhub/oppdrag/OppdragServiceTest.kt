package no.decisive.staffhub.oppdrag

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.felles.OverlappException
import no.decisive.staffhub.felles.UgyldigStatusOvergangException
import no.decisive.staffhub.felles.ValideringException
import no.decisive.staffhub.konsulent.Konsulent
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.oppdrag.Oppdrag.Status
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
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

class OppdragServiceTest {

    private val oppdragRepository = mock<OppdragRepository>()
    private val konsulentRepository = mock<KonsulentRepository>()
    private val oppdragService = OppdragService(oppdragRepository, konsulentRepository)

    @Test
    fun `skal opprette oppdrag`() {
        // given
        val konsulent = lagTestKonsulent()
        val oppdrag = lagTestOppdrag()
        whenever(konsulentRepository.hentPåId(1L)).thenReturn(konsulent)

        // when
        val resultat = oppdragService.opprett(oppdrag)

        // then
        assertThat(resultat.tittel).isEqualTo("Modernisering")
        assertThat(resultat.status).isEqualTo(Status.FORESLÅTT)
        verify(oppdragRepository).lagre(any())
        verify(konsulentRepository).hentPåId(1L)
    }

    @Test
    fun `skal endre status fra FORESLÅTT til BEKREFTET`() {
        // given
        val oppdrag = lagTestOppdrag()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)

        // when
        val resultat = oppdragService.endreStatus(1L, Status.BEKREFTET)

        // then
        assertThat(resultat.status).isEqualTo(Status.BEKREFTET)
        verify(oppdragRepository).lagre(any())
    }

    @Test
    fun `skal endre status fra BEKREFTET til AKTIV uten overlapp`() {
        // given
        val oppdrag = lagTestOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)
        whenever(oppdragRepository.finnAktiveForKonsulent(1L)).thenReturn(emptyList())

        // when
        val resultat = oppdragService.endreStatus(1L, Status.AKTIV)

        // then
        assertThat(resultat.status).isEqualTo(Status.AKTIV)
    }

    @Test
    fun `skal feile ved ugyldig statusovergang`() {
        // given
        val oppdrag = lagTestOppdrag()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)

        // when/then
        assertThatThrownBy { oppdragService.endreStatus(1L, Status.FULLFØRT) }
            .isInstanceOf(UgyldigStatusOvergangException::class.java)
    }

    @Test
    fun `skal feile ved overlappende aktive oppdrag`() {
        // given
        val oppdrag = lagTestOppdrag()
        oppdrag.endreStatus(Status.BEKREFTET)
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)

        val aktivtOppdrag = lagTestOppdrag(id = 2L)
        aktivtOppdrag.endreStatus(Status.BEKREFTET)
        aktivtOppdrag.endreStatus(Status.AKTIV)
        whenever(oppdragRepository.finnAktiveForKonsulent(1L)).thenReturn(listOf(aktivtOppdrag))

        // when/then
        assertThatThrownBy { oppdragService.endreStatus(1L, Status.AKTIV) }
            .isInstanceOf(OverlappException::class.java)
    }

    @Test
    fun `skal feile ved opprettelse når konsulent ikke finnes`() {
        // given
        val oppdrag = lagTestOppdrag()
        whenever(konsulentRepository.hentPåId(1L)).thenThrow(IkkeFunnetException("Konsulent med id 1 ikke funnet"))

        // when/then
        assertThatThrownBy { oppdragService.opprett(oppdrag) }
            .isInstanceOf(ValideringException::class.java)
        verify(oppdragRepository, never()).lagre(any())
    }

    @Test
    fun `skal kansellere foreslått oppdrag`() {
        // given
        val oppdrag = lagTestOppdrag()
        whenever(oppdragRepository.hentPåId(1L)).thenReturn(oppdrag)

        // when
        val resultat = oppdragService.endreStatus(1L, Status.KANSELLERT)

        // then
        assertThat(resultat.status).isEqualTo(Status.KANSELLERT)
    }

    private fun lagTestOppdrag(id: Long = 1L): Oppdrag {
        val mockIdProvider = mock<IdProvider> {
            on { nesteOppdragId() }.thenReturn(id)
        }
        return Oppdrag(
            idProvider = mockIdProvider,
            tittel = "Modernisering",
            kundeNavn = "Acme AS",
            beskrivelse = "Modernisere backend",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 6, 30),
            timepris = BigDecimal("1500.00"),
            konsulentId = 1L
        )
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
