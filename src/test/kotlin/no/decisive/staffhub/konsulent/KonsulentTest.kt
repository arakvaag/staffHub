package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class KonsulentTest {

    private fun lagIdProvider(): IdProvider {
        var konsulentSeq = 0L
        var kompetanseSeq = 0L
        return mock<IdProvider> {
            on { nesteKonsulentId() }.thenAnswer { ++konsulentSeq }
            on { nesteKompetanseId() }.thenAnswer { ++kompetanseSeq }
        }
    }

    @Test
    fun `skal opprette konsulent med idProvider`() {
        val idProvider = lagIdProvider()
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            telefon = "12345678"
        )

        assertThat(konsulent.id).isEqualTo(1L)
        assertThat(konsulent.fornavn).isEqualTo("Ola")
        assertThat(konsulent.etternavn).isEqualTo("Nordmann")
        assertThat(konsulent.epost).isEqualTo("ola@firma.no")
        assertThat(konsulent.telefon).isEqualTo("12345678")
        assertThat(konsulent.opprettetDato).isNotNull()
        assertThat(konsulent.kompetanser).isEmpty()
    }

    @Test
    fun `telefon skal ha default null`() {
        val konsulent = Konsulent(
            idProvider = lagIdProvider(),
            fornavn = "Kari",
            etternavn = "Hansen",
            epost = "kari@firma.no"
        )

        assertThat(konsulent.telefon).isNull()
    }

    @Test
    fun `skal opprette konsulent med kompetanser`() {
        val idProvider = lagIdProvider()
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR,
            beskrivelse = "Kotlin"
        )
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            kompetanser = listOf(kompetanse)
        )

        assertThat(konsulent.kompetanser).hasSize(1)
        assertThat(konsulent.kompetanser[0].fagområde).isEqualTo(Fagområde.BACKEND)
    }

    @Test
    fun `skal legge til kompetanse`() {
        val idProvider = lagIdProvider()
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.FRONTEND,
            nivå = Kompetansenivå.JUNIOR
        )

        konsulent.leggTilKompetanse(kompetanse)

        assertThat(konsulent.kompetanser).hasSize(1)
        assertThat(konsulent.kompetanser[0].fagområde).isEqualTo(Fagområde.FRONTEND)
    }

    @Test
    fun `ny konsulent skal være markert som ny`() {
        val konsulent = Konsulent(
            idProvider = lagIdProvider(),
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )

        assertThat(konsulent.erNy).isTrue()
        assertThat(konsulent.erEndret).isFalse()
    }

    @Test
    fun `konsulent skal ikke være ny etter bekreftPersistert`() {
        val konsulent = Konsulent(
            idProvider = lagIdProvider(),
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )

        konsulent.bekreftPersistert()

        assertThat(konsulent.erNy).isFalse()
        assertThat(konsulent.erEndret).isFalse()
    }

    @Test
    fun `konsulent skal være markert som endret etter feltendring`() {
        val konsulent = Konsulent(
            idProvider = lagIdProvider(),
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        konsulent.bekreftPersistert()

        konsulent.fornavn = "Per"

        assertThat(konsulent.erNy).isFalse()
        assertThat(konsulent.erEndret).isTrue()
    }

    @Test
    fun `konsulent skal være markert som endret etter ny kompetanse`() {
        val idProvider = lagIdProvider()
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        konsulent.bekreftPersistert()

        konsulent.leggTilKompetanse(
            Kompetanse(idProvider = idProvider, fagområde = Fagområde.BACKEND, nivå = Kompetansenivå.SENIOR)
        )

        assertThat(konsulent.erEndret).isTrue()
    }

    @Test
    fun `konsulent skal ikke være endret etter bekreftPersistert på nytt`() {
        val konsulent = Konsulent(
            idProvider = lagIdProvider(),
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        konsulent.bekreftPersistert()
        konsulent.fornavn = "Per"

        konsulent.bekreftPersistert()

        assertThat(konsulent.erEndret).isFalse()
    }

    @Test
    fun `bekreftPersistert skal også bekrefte kompetanser`() {
        val idProvider = lagIdProvider()
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR
        )
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            kompetanser = listOf(kompetanse)
        )

        assertThat(kompetanse.erNy).isTrue()

        konsulent.bekreftPersistert()

        assertThat(kompetanse.erNy).isFalse()
    }

    @Test
    fun `konsulent fra PersistertState skal ikke være ny`() {
        val state = Konsulent.PersistertState(
            id = 1L,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            telefon = "12345678",
            opprettetDato = LocalDateTime.of(2026, 1, 1, 12, 0),
            kompetanser = listOf(
                Kompetanse.PersistertState(
                    id = 10L,
                    fagområde = Fagområde.BACKEND,
                    nivå = Kompetansenivå.SENIOR,
                    beskrivelse = "Kotlin"
                )
            )
        )

        val konsulent = Konsulent.fra(state)

        assertThat(konsulent.erNy).isFalse()
        assertThat(konsulent.erEndret).isFalse()
        assertThat(konsulent.id).isEqualTo(1L)
        assertThat(konsulent.fornavn).isEqualTo("Ola")
        assertThat(konsulent.kompetanser).hasSize(1)
        assertThat(konsulent.kompetanser[0].erNy).isFalse()
    }

}
