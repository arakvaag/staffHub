package no.decisive.staffhub.komponenttest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class OppdragApiKomponentTest : KomponentTest() {

    private val objectMapper = JsonMapper.builder().addModule(kotlinModule()).build()

    private var konsulentId: Long = 0

    @BeforeEach
    fun opprettKonsulent() {
        val response = mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Test",
                        "etternavn": "Konsulent",
                        "epost": "test@firma.no"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        konsulentId = objectMapper.readTree(response).get("id").asLong()
    }

    @Test
    fun `skal opprette oppdrag`() {
        val response = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Systemutvikling",
                        "kundeNavn": "Acme AS",
                        "beskrivelse": "Backend-utvikling i Kotlin",
                        "startDato": "2026-05-01",
                        "sluttDato": "2026-08-31",
                        "timepris": 1200,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            {
                "tittel": "Systemutvikling",
                "kundeNavn": "Acme AS",
                "beskrivelse": "Backend-utvikling i Kotlin",
                "startDato": "2026-05-01",
                "sluttDato": "2026-08-31",
                "status": "FORESLÅTT",
                "timepris": 1200,
                "konsulentId": $konsulentId
            }
            """.trimIndent(),
            response,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `skal endre status på oppdrag`() {
        // Opprett oppdrag
        val opprettResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Rådgivning",
                        "kundeNavn": "Bedrift AS",
                        "startDato": "2026-06-01",
                        "sluttDato": "2026-09-30",
                        "timepris": 1500,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val oppdragId = objectMapper.readTree(opprettResponse).get("id").asLong()

        // Endre status til BEKREFTET
        val response = mockMvc.perform(
            put("/api/oppdrag/$oppdragId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            {
                "tittel": "Rådgivning",
                "kundeNavn": "Bedrift AS",
                "status": "BEKREFTET",
                "konsulentId": $konsulentId
            }
            """.trimIndent(),
            response,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `skal returnere 400 når konsulent ikke finnes`() {
        mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Prosjekt",
                        "kundeNavn": "Kunde AS",
                        "startDato": "2026-05-01",
                        "sluttDato": "2026-08-31",
                        "timepris": 1200,
                        "konsulentId": 999999
                    }
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 400 ved ugyldig statusovergang`() {
        val opprettResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Prosjekt",
                        "kundeNavn": "Kunde AS",
                        "startDato": "2026-05-01",
                        "sluttDato": "2026-08-31",
                        "timepris": 1200,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val oppdragId = objectMapper.readTree(opprettResponse).get("id").asLong()

        // FORESLÅTT -> FULLFØRT er ikke en gyldig overgang
        mockMvc.perform(
            put("/api/oppdrag/$oppdragId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "FULLFØRT" }""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 409 ved overlappende aktive oppdrag`() {
        // Opprett første oppdrag og aktiver det
        val førsteResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Første oppdrag",
                        "kundeNavn": "Kunde AS",
                        "startDato": "2026-05-01",
                        "sluttDato": "2026-08-31",
                        "timepris": 1200,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val førsteId = objectMapper.readTree(førsteResponse).get("id").asLong()

        mockMvc.perform(
            put("/api/oppdrag/$førsteId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/oppdrag/$førsteId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        ).andExpect(status().isOk)

        // Opprett andre oppdrag med overlappende datoer
        val andreResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Andre oppdrag",
                        "kundeNavn": "Annen Kunde AS",
                        "startDato": "2026-06-01",
                        "sluttDato": "2026-10-31",
                        "timepris": 1500,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val andreId = objectMapper.readTree(andreResponse).get("id").asLong()

        mockMvc.perform(
            put("/api/oppdrag/$andreId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        // Forsøk å aktivere det andre — skal gi 409 pga overlapp
        mockMvc.perform(
            put("/api/oppdrag/$andreId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `skal feile ved ugyldig oppdrag-request`() {
        mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "tittel": "", "kundeNavn": "" }""")
        )
            .andExpect(status().isBadRequest)
    }
}
