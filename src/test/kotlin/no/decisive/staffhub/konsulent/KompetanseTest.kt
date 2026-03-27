package no.decisive.staffhub.konsulent

import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class KompetanseTest {

    private val idProvider = mock<IdProvider> {
        on { nesteKompetanseId() }.thenReturn(1L)
    }

    @Test
    fun `skal opprette kompetanse med idProvider`() {
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR,
            beskrivelse = "Kotlin"
        )

        assertThat(kompetanse.id).isEqualTo(1L)
        assertThat(kompetanse.fagområde).isEqualTo(Fagområde.BACKEND)
        assertThat(kompetanse.nivå).isEqualTo(Kompetansenivå.SENIOR)
        assertThat(kompetanse.beskrivelse).isEqualTo("Kotlin")
    }

    @Test
    fun `beskrivelse skal ha default null`() {
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.FRONTEND,
            nivå = Kompetansenivå.JUNIOR
        )

        assertThat(kompetanse.beskrivelse).isNull()
    }

    @Test
    fun `ny kompetanse skal være markert som ny`() {
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR
        )

        assertThat(kompetanse.erNy).isTrue()
        assertThat(kompetanse.erEndret).isFalse()
    }

    @Test
    fun `kompetanse skal ikke være ny etter bekreftPersistert`() {
        val kompetanse = Kompetanse(
            idProvider = idProvider,
            fagområde = Fagområde.BACKEND,
            nivå = Kompetansenivå.SENIOR
        )

        kompetanse.bekreftPersistert()

        assertThat(kompetanse.erNy).isFalse()
        assertThat(kompetanse.erEndret).isFalse()
    }

    @Test
    fun `kompetanse fra PersistertState skal ikke være ny`() {
        val state = Kompetanse.PersistertState(
            id = 1L,
            fagområde = Fagområde.DEVOPS,
            nivå = Kompetansenivå.EKSPERT,
            beskrivelse = "Kubernetes"
        )

        val kompetanse = Kompetanse.fra(state)

        assertThat(kompetanse.erNy).isFalse()
        assertThat(kompetanse.erEndret).isFalse()
        assertThat(kompetanse.id).isEqualTo(1L)
        assertThat(kompetanse.fagområde).isEqualTo(Fagområde.DEVOPS)
        assertThat(kompetanse.nivå).isEqualTo(Kompetansenivå.EKSPERT)
        assertThat(kompetanse.beskrivelse).isEqualTo("Kubernetes")
    }
}
