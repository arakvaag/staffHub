package no.decisive.staffhub.persistering

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.konsulent.Konsulent
import no.decisive.staffhub.konsulent.persistering.KompetanseTabell
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.konsulent.persistering.KonsulentTabell
import no.decisive.staffhub.oppdrag.Oppdrag
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
import no.decisive.staffhub.oppdrag.persistering.OppdragTabell
import no.decisive.staffhub.timeføring.Timeregistrering
import no.decisive.staffhub.timeføring.persistering.TimeregistreringRepository
import no.decisive.staffhub.timeføring.persistering.TimeregistreringTabell
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TimeregistreringRepositoryTest : DatabaseTest() {

    private lateinit var idProvider: IdProvider
    private lateinit var timeregistreringRepository: TimeregistreringRepository
    private var konsulentId: Long = 0
    private var oppdragId: Long = 0

    @BeforeEach
    fun setUpRepository() {
        idProvider = IdProvider(jdbc)
        val konsulentRepository = KonsulentRepository(KonsulentTabell(jdbc), KompetanseTabell(jdbc))
        val oppdragRepository = OppdragRepository(OppdragTabell(jdbc))
        timeregistreringRepository = TimeregistreringRepository(TimeregistreringTabell(jdbc))

        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        konsulentRepository.lagre(konsulent)
        konsulentId = konsulent.id

        val oppdrag = Oppdrag(
            idProvider = idProvider,
            tittel = "Modernisering",
            kundeNavn = "Acme AS",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31),
            timepris = BigDecimal("1200.00"),
            konsulentId = konsulentId
        )
        oppdragRepository.lagre(oppdrag)
        oppdragId = oppdrag.id
    }

    private fun lagTimeregistrering(
        dato: LocalDate = LocalDate.of(2026, 6, 15),
        timer: Int = 7,
        minutter: Int = 30,
    ) = Timeregistrering(
        idProvider = idProvider,
        oppdragId = oppdragId,
        konsulentId = konsulentId,
        dato = dato,
        timer = timer,
        minutter = minutter,
        beskrivelse = "Backend-utvikling"
    )

    @Test
    fun `skal lagre og hente timeregistrering`() {
        val timeregistrering = lagTimeregistrering()

        timeregistreringRepository.lagre(timeregistrering)
        val hentet = timeregistreringRepository.hentPåId(timeregistrering.id)

        assertThat(hentet.id).isEqualTo(timeregistrering.id)
        assertThat(hentet.oppdragId).isEqualTo(oppdragId)
        assertThat(hentet.konsulentId).isEqualTo(konsulentId)
        assertThat(hentet.dato).isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(hentet.timer).isEqualTo(7)
        assertThat(hentet.minutter).isEqualTo(30)
        assertThat(hentet.beskrivelse).isEqualTo("Backend-utvikling")
        assertThat(hentet.opprettetDato).isEqualTo(timeregistrering.opprettetDato)
    }

    @Test
    fun `skal kaste IkkeFunnetException for ukjent id`() {
        assertThatThrownBy { timeregistreringRepository.hentPåId(999L) }
            .isInstanceOf(IkkeFunnetException::class.java)
    }

    @Test
    fun `skal finne alle timeregistreringer`() {
        timeregistreringRepository.lagre(lagTimeregistrering(dato = LocalDate.of(2026, 6, 15)))
        timeregistreringRepository.lagre(lagTimeregistrering(dato = LocalDate.of(2026, 6, 16)))

        val alle = timeregistreringRepository.finnAlle()

        assertThat(alle).hasSize(2)
    }

    @Test
    fun `finnAlle skal returnere tom liste når ingen finnes`() {
        val alle = timeregistreringRepository.finnAlle()

        assertThat(alle).isEmpty()
    }

    @Test
    fun `skal finne timeregistreringer for oppdrag`() {
        val oppdragRepository = OppdragRepository(OppdragTabell(jdbc))

        val oppdrag2 = Oppdrag(
            idProvider = idProvider,
            tittel = "Annet prosjekt",
            kundeNavn = "Annen Kunde AS",
            startDato = LocalDate.of(2027, 1, 1),
            sluttDato = LocalDate.of(2027, 12, 31),
            timepris = BigDecimal("1500.00"),
            konsulentId = konsulentId
        )
        oppdragRepository.lagre(oppdrag2)

        timeregistreringRepository.lagre(lagTimeregistrering())
        timeregistreringRepository.lagre(
            Timeregistrering(
                idProvider = idProvider,
                oppdragId = oppdrag2.id,
                konsulentId = konsulentId,
                dato = LocalDate.of(2027, 6, 15),
                timer = 4,
                minutter = 0,
                beskrivelse = "Annet arbeid"
            )
        )

        val forOppdrag = timeregistreringRepository.finnAlleForOppdrag(oppdragId)

        assertThat(forOppdrag).hasSize(1)
        assertThat(forOppdrag[0].oppdragId).isEqualTo(oppdragId)
    }

    @Test
    fun `skal finne timeregistreringer for konsulent`() {
        val konsulentRepository = KonsulentRepository(KonsulentTabell(jdbc), KompetanseTabell(jdbc))
        val oppdragRepository = OppdragRepository(OppdragTabell(jdbc))

        val konsulent2 = Konsulent(
            idProvider = idProvider,
            fornavn = "Kari",
            etternavn = "Nordmann",
            epost = "kari@firma.no"
        )
        konsulentRepository.lagre(konsulent2)

        val oppdrag2 = Oppdrag(
            idProvider = idProvider,
            tittel = "Annet prosjekt",
            kundeNavn = "Annen Kunde AS",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31),
            timepris = BigDecimal("1500.00"),
            konsulentId = konsulent2.id
        )
        oppdragRepository.lagre(oppdrag2)

        timeregistreringRepository.lagre(lagTimeregistrering())
        timeregistreringRepository.lagre(
            Timeregistrering(
                idProvider = idProvider,
                oppdragId = oppdrag2.id,
                konsulentId = konsulent2.id,
                dato = LocalDate.of(2026, 6, 15),
                timer = 4,
                minutter = 0,
                beskrivelse = "Annet arbeid"
            )
        )

        val forKonsulent = timeregistreringRepository.finnAlleForKonsulent(konsulentId)

        assertThat(forKonsulent).hasSize(1)
        assertThat(forKonsulent[0].konsulentId).isEqualTo(konsulentId)
    }

    @Test
    fun `skal finne timeregistreringer for oppdrag og konsulent`() {
        val konsulentRepository = KonsulentRepository(KonsulentTabell(jdbc), KompetanseTabell(jdbc))
        val oppdragRepository = OppdragRepository(OppdragTabell(jdbc))

        val konsulent2 = Konsulent(
            idProvider = idProvider,
            fornavn = "Kari",
            etternavn = "Nordmann",
            epost = "kari2@firma.no"
        )
        konsulentRepository.lagre(konsulent2)

        val oppdrag2 = Oppdrag(
            idProvider = idProvider,
            tittel = "Annet prosjekt",
            kundeNavn = "Annen Kunde AS",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31),
            timepris = BigDecimal("1500.00"),
            konsulentId = konsulent2.id
        )
        oppdragRepository.lagre(oppdrag2)

        // Tre registreringer: to på oppdrag1, én på oppdrag2
        timeregistreringRepository.lagre(lagTimeregistrering(dato = LocalDate.of(2026, 6, 15)))
        timeregistreringRepository.lagre(lagTimeregistrering(dato = LocalDate.of(2026, 6, 16)))
        timeregistreringRepository.lagre(
            Timeregistrering(
                idProvider = idProvider,
                oppdragId = oppdrag2.id,
                konsulentId = konsulent2.id,
                dato = LocalDate.of(2026, 6, 15),
                timer = 4,
                minutter = 0,
                beskrivelse = "Annet arbeid"
            )
        )

        val resultat = timeregistreringRepository.finnAlleForOppdragOgKonsulent(oppdragId, konsulentId)

        assertThat(resultat).hasSize(2)
        assertThat(resultat).allMatch { it.oppdragId == oppdragId && it.konsulentId == konsulentId }
    }

    @Test
    fun `lagret timeregistrering skal ikke lenger være markert som ny`() {
        val timeregistrering = lagTimeregistrering()
        assertThat(timeregistrering.erNy).isTrue()

        timeregistreringRepository.lagre(timeregistrering)

        assertThat(timeregistrering.erNy).isFalse()
        assertThat(timeregistrering.erEndret).isFalse()
    }

    @Test
    fun `timeregistrering hentet fra database skal ikke være markert som ny eller endret`() {
        val timeregistrering = lagTimeregistrering()
        timeregistreringRepository.lagre(timeregistrering)

        val hentet = timeregistreringRepository.hentPåId(timeregistrering.id)

        assertThat(hentet.erNy).isFalse()
        assertThat(hentet.erEndret).isFalse()
    }
}
