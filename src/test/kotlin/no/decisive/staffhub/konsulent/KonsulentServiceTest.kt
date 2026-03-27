package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KonsulentServiceTest {

    private val konsulentRepository = mock<KonsulentRepository>()
    private val konsulentService = KonsulentService(konsulentRepository)

    @Test
    fun `skal opprette konsulent`() {
        // given
        val konsulent = lagTestKonsulent()

        // when
        val resultat = konsulentService.opprett(konsulent)

        // then
        assertThat(resultat.fornavn).isEqualTo("Ola")
        assertThat(resultat.etternavn).isEqualTo("Nordmann")
        verify(konsulentRepository).lagre(any())
    }

    @Test
    fun `skal opprette konsulent med kompetanser`() {
        // given
        val idProvider = lagTestIdProvider()
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR,
            beskrivelse = "Kotlin og Spring Boot"
        )
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Kari",
            etternavn = "Hansen",
            epost = "kari@firma.no",
            kompetanser = listOf(kompetanse)
        )

        // when
        val resultat = konsulentService.opprett(konsulent)

        // then
        assertThat(resultat.kompetanser).hasSize(1)
        assertThat(resultat.kompetanser[0].fagområde).isEqualTo(Fagområde.BACKEND)
        verify(konsulentRepository).lagre(any())
    }

    @Test
    fun `skal hente alle konsulenter`() {
        // given
        val konsulenter = listOf(lagTestKonsulent())
        whenever(konsulentRepository.finnAlle()).thenReturn(konsulenter)

        // when
        val resultat = konsulentService.hentAlle()

        // then
        assertThat(resultat).hasSize(1)
        verify(konsulentRepository).finnAlle()
    }

    @Test
    fun `skal filtrere konsulenter på fagområde`() {
        // given
        val konsulenter = listOf(lagTestKonsulent())
        whenever(konsulentRepository.finnAlleMedFagområde(Fagområde.BACKEND))
            .thenReturn(konsulenter)

        // when
        val resultat = konsulentService.hentAlle(Fagområde.BACKEND)

        // then
        assertThat(resultat).hasSize(1)
        verify(konsulentRepository).finnAlleMedFagområde(Fagområde.BACKEND)
    }

    private fun lagTestIdProvider(): IdProvider {
        var konsulentIdSeq = 0L
        var kompetanseIdSeq = 0L
        return mock<IdProvider> {
            on { nesteKonsulentId() }.thenAnswer { ++konsulentIdSeq }
            on { nesteKompetanseId() }.thenAnswer { ++kompetanseIdSeq }
        }
    }

    private fun lagTestKonsulent(): Konsulent {
        val idProvider = lagTestIdProvider()
        return Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            telefon = "12345678"
        )
    }
}
