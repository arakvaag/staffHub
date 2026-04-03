package no.decisive.staffhub.komponenttest

import no.decisive.staffhub.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
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
class KonsulentApiKomponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `skal opprette og hente konsulent`() {
        val request = """
            {
                "fornavn": "Ola",
                "etternavn": "Nordmann",
                "epost": "ola.komponent@firma.no",
                "telefon": "12345678",
                "kompetanser": [
                    {
                        "fagområde": "BACKEND",
                        "nivå": "SENIOR",
                        "beskrivelse": "Kotlin"
                    }
                ]
            }
        """.trimIndent()

        // Opprett konsulent
        val opprettResponse = mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            {
                "fornavn": "Ola",
                "etternavn": "Nordmann",
                "epost": "ola.komponent@firma.no",
                "telefon": "12345678",
                "kompetanser": [
                    {
                        "fagområde": "BACKEND",
                        "nivå": "SENIOR",
                        "beskrivelse": "Kotlin"
                    }
                ]
            }
            """.trimIndent(),
            opprettResponse,
            JSONCompareMode.LENIENT
        )

        // Hent alle konsulenter
        val hentResponse = mockMvc.perform(get("/api/konsulenter"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            [
                {
                    "fornavn": "Ola",
                    "etternavn": "Nordmann",
                    "epost": "ola.komponent@firma.no",
                    "kompetanser": [
                        {
                            "fagområde": "BACKEND",
                            "nivå": "SENIOR",
                            "beskrivelse": "Kotlin"
                        }
                    ]
                }
            ]
            """.trimIndent(),
            hentResponse,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `skal filtrere konsulenter på fagområde`() {
        // Opprett backend-konsulent
        mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Kari",
                        "etternavn": "Hansen",
                        "epost": "kari.filter@firma.no",
                        "kompetanser": [
                            { "fagområde": "BACKEND", "nivå": "SENIOR" }
                        ]
                    }
                """.trimIndent())
        ).andExpect(status().isCreated)

        // Opprett frontend-konsulent
        mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Per",
                        "etternavn": "Olsen",
                        "epost": "per.filter@firma.no",
                        "kompetanser": [
                            { "fagområde": "FRONTEND", "nivå": "JUNIOR" }
                        ]
                    }
                """.trimIndent())
        ).andExpect(status().isCreated)

        // Filtrer på FRONTEND
        val response = mockMvc.perform(get("/api/konsulenter?fagområde=FRONTEND"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            [
                {
                    "fornavn": "Per",
                    "etternavn": "Olsen",
                    "epost": "per.filter@firma.no",
                    "kompetanser": [
                        { "fagområde": "FRONTEND", "nivå": "JUNIOR" }
                    ]
                }
            ]
            """.trimIndent(),
            response,
            JSONCompareMode.LENIENT
        )
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
