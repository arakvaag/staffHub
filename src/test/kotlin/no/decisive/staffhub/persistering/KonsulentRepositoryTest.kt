package no.decisive.staffhub.persistering

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.konsulent.Kompetanse
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import no.decisive.staffhub.konsulent.Konsulent
import no.decisive.staffhub.konsulent.persistering.KompetanseTabell
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.konsulent.persistering.KonsulentTabell
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KonsulentRepositoryTest : DatabaseTest() {

    private lateinit var idProvider: IdProvider
    private lateinit var repository: KonsulentRepository

    @BeforeEach
    fun setUpRepository() {
        idProvider = IdProvider(jdbc)
        repository = KonsulentRepository(
            KonsulentTabell(jdbc),
            KompetanseTabell(jdbc)
        )
    }

    @Test
    fun `skal lagre og hente konsulent`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            telefon = "12345678"
        )

        repository.lagre(konsulent)
        val hentet = repository.hentPåId(konsulent.id)

        assertThat(hentet.id).isEqualTo(konsulent.id)
        assertThat(hentet.fornavn).isEqualTo("Ola")
        assertThat(hentet.etternavn).isEqualTo("Nordmann")
        assertThat(hentet.epost).isEqualTo("ola@firma.no")
        assertThat(hentet.telefon).isEqualTo("12345678")
        assertThat(hentet.opprettetDato).isEqualTo(konsulent.opprettetDato)
    }

    @Test
    fun `skal lagre konsulent uten telefon`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Kari",
            etternavn = "Hansen",
            epost = "kari@firma.no"
        )

        repository.lagre(konsulent)
        val hentet = repository.hentPåId(konsulent.id)

        assertThat(hentet.telefon).isNull()
    }

    @Test
    fun `skal lagre konsulent med kompetanser`() {
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

        repository.lagre(konsulent)
        val hentet = repository.hentPåId(konsulent.id)

        assertThat(hentet.kompetanser).hasSize(1)
        assertThat(hentet.kompetanser[0].fagområde).isEqualTo(Fagområde.BACKEND)
        assertThat(hentet.kompetanser[0].nivå).isEqualTo(Kompetansenivå.SENIOR)
        assertThat(hentet.kompetanser[0].beskrivelse).isEqualTo("Kotlin")
    }

    @Test
    fun `skal oppdatere konsulent`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        repository.lagre(konsulent)

        konsulent.fornavn = "Per"
        konsulent.etternavn = "Olsen"
        repository.lagre(konsulent)

        val hentet = repository.hentPåId(konsulent.id)
        assertThat(hentet.fornavn).isEqualTo("Per")
        assertThat(hentet.etternavn).isEqualTo("Olsen")
    }

    @Test
    fun `skal legge til ny kompetanse på eksisterende konsulent`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        repository.lagre(konsulent)

        konsulent.leggTilKompetanse(
            Kompetanse(idProvider = idProvider, fagområde = Fagområde.FRONTEND, nivå = Kompetansenivå.JUNIOR)
        )
        repository.lagre(konsulent)

        val hentet = repository.hentPåId(konsulent.id)
        assertThat(hentet.kompetanser).hasSize(1)
        assertThat(hentet.kompetanser[0].fagområde).isEqualTo(Fagområde.FRONTEND)
    }

    @Test
    fun `skal kaste IkkeFunnetException for ukjent id`() {
        assertThatThrownBy { repository.hentPåId(999L) }
            .isInstanceOf(IkkeFunnetException::class.java)
    }

    @Test
    fun `skal finne alle konsulenter`() {
        repository.lagre(
            Konsulent(idProvider = idProvider, fornavn = "Ola", etternavn = "Nordmann", epost = "ola@firma.no")
        )
        repository.lagre(
            Konsulent(idProvider = idProvider, fornavn = "Kari", etternavn = "Hansen", epost = "kari@firma.no")
        )

        val alle = repository.finnAlle()

        assertThat(alle).hasSize(2)
    }

    @Test
    fun `finnAlle skal returnere tom liste når ingen konsulenter finnes`() {
        val alle = repository.finnAlle()

        assertThat(alle).isEmpty()
    }

    @Test
    fun `skal finne konsulenter med gitt fagområde`() {
        val backend = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no",
            kompetanser = listOf(
                Kompetanse(idProvider = idProvider, fagområde = Fagområde.BACKEND, nivå = Kompetansenivå.SENIOR)
            )
        )
        val frontend = Konsulent(
            idProvider = idProvider,
            fornavn = "Kari",
            etternavn = "Hansen",
            epost = "kari@firma.no",
            kompetanser = listOf(
                Kompetanse(idProvider = idProvider, fagområde = Fagområde.FRONTEND, nivå = Kompetansenivå.JUNIOR)
            )
        )
        repository.lagre(backend)
        repository.lagre(frontend)

        val backendKonsulenter = repository.finnAlleMedFagområde(Fagområde.BACKEND)

        assertThat(backendKonsulenter).hasSize(1)
        assertThat(backendKonsulenter[0].fornavn).isEqualTo("Ola")
    }

    @Test
    fun `finnAlleMedFagområde skal returnere tom liste når ingen matcher`() {
        val resultat = repository.finnAlleMedFagområde(Fagområde.DEVOPS)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `konsulent hentet fra database skal ikke være markert som ny eller endret`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        repository.lagre(konsulent)

        val hentet = repository.hentPåId(konsulent.id)

        assertThat(hentet.erNy).isFalse()
        assertThat(hentet.erEndret).isFalse()
    }

    @Test
    fun `lagret konsulent skal ikke lenger være markert som ny`() {
        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        assertThat(konsulent.erNy).isTrue()

        repository.lagre(konsulent)

        assertThat(konsulent.erNy).isFalse()
        assertThat(konsulent.erEndret).isFalse()
    }
}
