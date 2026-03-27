package no.decisive.staffhub.konsulent

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import no.decisive.staffhub.TestcontainersConfiguration
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.Kompetanse.Kompetansenivå
import no.decisive.staffhub.konsulent.api.OpprettKompetanseRequest
import no.decisive.staffhub.konsulent.api.OpprettKonsulentRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class KonsulentControllerKomponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = JsonMapper.builder().addModule(kotlinModule()).build()

    @Test
    fun `skal opprette og hente konsulent`() {
        val request = OpprettKonsulentRequest(
            fornavn = "Ola",
            etternavn = "Nordmann",
            epost = "ola.komponent@firma.no",
            telefon = "12345678",
            kompetanser = listOf(
                OpprettKompetanseRequest(
                    fagområde = Fagområde.BACKEND,
                    nivå = Kompetansenivå.SENIOR,
                    beskrivelse = "Kotlin"
                )
            )
        )

        // Opprett konsulent
        mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fornavn").value("Ola"))
            .andExpect(jsonPath("$.etternavn").value("Nordmann"))
            .andExpect(jsonPath("$.epost").value("ola.komponent@firma.no"))
            .andExpect(jsonPath("$.kompetanser[0].fagområde").value("BACKEND"))
            .andExpect(jsonPath("$.kompetanser[0].nivå").value("SENIOR"))

        // Hent alle konsulenter
        mockMvc.perform(get("/api/konsulenter"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[?(@.epost == 'ola.komponent@firma.no')].fornavn").value("Ola"))
    }

    @Test
    fun `skal feile ved ugyldig request`() {
        val ugyldigRequest = """{"fornavn": "", "etternavn": "", "epost": "ugyldig"}"""

        mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ugyldigRequest)
        )
            .andExpect(status().isBadRequest)
    }
}
