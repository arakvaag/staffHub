package no.decisive.staffhub.persistering

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.felles.IkkeFunnetException
import no.decisive.staffhub.konsulent.Konsulent
import no.decisive.staffhub.konsulent.persistering.KompetanseTabell
import no.decisive.staffhub.konsulent.persistering.KonsulentRepository
import no.decisive.staffhub.konsulent.persistering.KonsulentTabell
import no.decisive.staffhub.oppdrag.Oppdrag
import no.decisive.staffhub.oppdrag.OppdragStatus
import no.decisive.staffhub.oppdrag.persistering.OppdragRepository
import no.decisive.staffhub.oppdrag.persistering.OppdragTabell
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OppdragRepositoryTest : DatabaseTest() {

    private lateinit var idProvider: IdProvider
    private lateinit var oppdragRepository: OppdragRepository
    private lateinit var konsulentRepository: KonsulentRepository
    private var konsulentId: Long = 0

    @BeforeEach
    fun setUpRepository() {
        idProvider = IdProvider(jdbc)
        oppdragRepository = OppdragRepository(OppdragTabell(jdbc))
        konsulentRepository = KonsulentRepository(KonsulentTabell(jdbc), KompetanseTabell(jdbc))

        val konsulent = Konsulent(
            idProvider = idProvider,
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola@firma.no"
        )
        konsulentRepository.lagre(konsulent)
        konsulentId = konsulent.id
    }

    private fun lagOppdrag(
        tittel: String = "Modernisering",
        startDato: LocalDate = LocalDate.of(2026, 1, 1),
        sluttDato: LocalDate = LocalDate.of(2026, 6, 30)
    ) = Oppdrag(
        idProvider = idProvider,
        tittel = tittel,
        kundeNavn = "Acme AS",
        beskrivelse = "Modernisere backend",
        startDato = startDato,
        sluttDato = sluttDato,
        timepris = BigDecimal("1500.00"),
        konsulentId = konsulentId
    )

    @Test
    fun `skal lagre og hente oppdrag`() {
        val oppdrag = lagOppdrag()

        oppdragRepository.lagre(oppdrag)
        val hentet = oppdragRepository.hentPåId(oppdrag.id)

        assertThat(hentet.id).isEqualTo(oppdrag.id)
        assertThat(hentet.tittel).isEqualTo("Modernisering")
        assertThat(hentet.kundeNavn).isEqualTo("Acme AS")
        assertThat(hentet.beskrivelse).isEqualTo("Modernisere backend")
        assertThat(hentet.startDato).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(hentet.sluttDato).isEqualTo(LocalDate.of(2026, 6, 30))
        assertThat(hentet.status).isEqualTo(OppdragStatus.FORESLÅTT)
        assertThat(hentet.timepris).isEqualByComparingTo(BigDecimal("1500.00"))
        assertThat(hentet.konsulentId).isEqualTo(konsulentId)
        assertThat(hentet.opprettetDato).isEqualTo(oppdrag.opprettetDato)
    }

    @Test
    fun `skal lagre oppdrag uten beskrivelse`() {
        val oppdrag = Oppdrag(
            idProvider = idProvider,
            tittel = "Kort oppdrag",
            kundeNavn = "Acme AS",
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 3, 31),
            timepris = BigDecimal("1200.00"),
            konsulentId = konsulentId
        )

        oppdragRepository.lagre(oppdrag)
        val hentet = oppdragRepository.hentPåId(oppdrag.id)

        assertThat(hentet.beskrivelse).isNull()
    }

    @Test
    fun `skal oppdatere oppdrag etter statusendring`() {
        val oppdrag = lagOppdrag()
        oppdragRepository.lagre(oppdrag)

        oppdrag.endreStatus(OppdragStatus.BEKREFTET)
        oppdragRepository.lagre(oppdrag)

        val hentet = oppdragRepository.hentPåId(oppdrag.id)
        assertThat(hentet.status).isEqualTo(OppdragStatus.BEKREFTET)
    }

    @Test
    fun `skal oppdatere oppdrag etter feltendring`() {
        val oppdrag = lagOppdrag()
        oppdragRepository.lagre(oppdrag)

        oppdrag.tittel = "Ny tittel"
        oppdrag.kundeNavn = "Nytt firma AS"
        oppdragRepository.lagre(oppdrag)

        val hentet = oppdragRepository.hentPåId(oppdrag.id)
        assertThat(hentet.tittel).isEqualTo("Ny tittel")
        assertThat(hentet.kundeNavn).isEqualTo("Nytt firma AS")
    }

    @Test
    fun `skal kaste IkkeFunnetException for ukjent id`() {
        assertThatThrownBy { oppdragRepository.hentPåId(999L) }
            .isInstanceOf(IkkeFunnetException::class.java)
    }

    @Test
    fun `skal finne alle oppdrag`() {
        oppdragRepository.lagre(lagOppdrag(tittel = "Oppdrag 1"))
        oppdragRepository.lagre(lagOppdrag(tittel = "Oppdrag 2"))

        val alle = oppdragRepository.finnAlle()

        assertThat(alle).hasSize(2)
    }

    @Test
    fun `finnAlle skal returnere tom liste når ingen oppdrag finnes`() {
        val alle = oppdragRepository.finnAlle()

        assertThat(alle).isEmpty()
    }

    @Test
    fun `skal finne aktive oppdrag for konsulent`() {
        val aktivt = lagOppdrag(tittel = "Aktivt")
        aktivt.endreStatus(OppdragStatus.BEKREFTET)
        aktivt.endreStatus(OppdragStatus.AKTIV)
        oppdragRepository.lagre(aktivt)

        val foreslått = lagOppdrag(
            tittel = "Foreslått",
            startDato = LocalDate.of(2026, 7, 1),
            sluttDato = LocalDate.of(2026, 12, 31)
        )
        oppdragRepository.lagre(foreslått)

        val aktive = oppdragRepository.finnAktiveForKonsulent(konsulentId)

        assertThat(aktive).hasSize(1)
        assertThat(aktive[0].tittel).isEqualTo("Aktivt")
    }

    @Test
    fun `finnAktiveForKonsulent skal returnere tom liste når ingen aktive finnes`() {
        oppdragRepository.lagre(lagOppdrag())

        val aktive = oppdragRepository.finnAktiveForKonsulent(konsulentId)

        assertThat(aktive).isEmpty()
    }

    @Test
    fun `oppdrag hentet fra database skal ikke være markert som nytt eller endret`() {
        val oppdrag = lagOppdrag()
        oppdragRepository.lagre(oppdrag)

        val hentet = oppdragRepository.hentPåId(oppdrag.id)

        assertThat(hentet.erNy).isFalse()
        assertThat(hentet.erEndret).isFalse()
    }

    @Test
    fun `lagret oppdrag skal ikke lenger være markert som nytt`() {
        val oppdrag = lagOppdrag()
        assertThat(oppdrag.erNy).isTrue()

        oppdragRepository.lagre(oppdrag)

        assertThat(oppdrag.erNy).isFalse()
        assertThat(oppdrag.erEndret).isFalse()
    }
}
