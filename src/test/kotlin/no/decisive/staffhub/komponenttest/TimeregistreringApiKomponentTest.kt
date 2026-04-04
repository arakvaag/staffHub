package no.decisive.staffhub.komponenttest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class TimeregistreringApiKomponentTest : KomponentTest() {

    private val objectMapper = JsonMapper.builder().addModule(kotlinModule()).build()

    private var konsulentId: Long = 0
    private var oppdragId: Long = 0

    @BeforeEach
    fun opprettTestdata() {
        // Opprett konsulent
        val konsulentResponse = mockMvc.perform(
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

        konsulentId = objectMapper.readTree(konsulentResponse).get("id").asLong()

        // Opprett oppdrag
        val oppdragResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Systemutvikling",
                        "kundeNavn": "Acme AS",
                        "startDato": "2026-01-01",
                        "sluttDato": "2026-12-31",
                        "timepris": 1200,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        oppdragId = objectMapper.readTree(oppdragResponse).get("id").asLong()

        // Aktiver oppdrag: FORESLÅTT → BEKREFTET → AKTIV
        mockMvc.perform(
            put("/api/oppdrag/$oppdragId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/oppdrag/$oppdragId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `skal opprette timeregistrering`() {
        val response = mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": $oppdragId,
                        "konsulentId": $konsulentId,
                        "dato": "2026-06-15",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            {
                "oppdragId": $oppdragId,
                "konsulentId": $konsulentId,
                "dato": "2026-06-15",
                "timer": 7,
                "minutter": 30,
                "beskrivelse": "Backend-utvikling",
                "totalMinutter": 450
            }
            """.trimIndent(),
            response,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `skal hente timeregistrering på id`() {
        val opprettResponse = opprettTimeregistrering()
        val id = objectMapper.readTree(opprettResponse).get("id").asLong()

        val response = mockMvc.perform(get("/api/timeregistreringer/$id"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        JSONAssert.assertEquals(
            """
            {
                "id": $id,
                "oppdragId": $oppdragId,
                "konsulentId": $konsulentId,
                "dato": "2026-06-15",
                "timer": 7,
                "minutter": 30,
                "beskrivelse": "Backend-utvikling",
                "totalMinutter": 450
            }
            """.trimIndent(),
            response,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `skal returnere 404 for ukjent timeregistrering`() {
        mockMvc.perform(get("/api/timeregistreringer/999999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `skal hente alle timeregistreringer`() {
        opprettTimeregistrering(dato = "2026-06-15")
        opprettTimeregistrering(dato = "2026-06-16")

        val response = mockMvc.perform(get("/api/timeregistreringer"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val array = objectMapper.readTree(response)
        assertThat(array.size()).isEqualTo(2)
    }

    @Test
    fun `skal filtrere timeregistreringer på oppdragId`() {
        // Opprett et andre oppdrag (ikke-overlappende datoer)
        val oppdrag2Response = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Annet prosjekt",
                        "kundeNavn": "Annen Kunde AS",
                        "startDato": "2027-01-01",
                        "sluttDato": "2027-12-31",
                        "timepris": 1500,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val oppdrag2Id = objectMapper.readTree(oppdrag2Response).get("id").asLong()

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        ).andExpect(status().isOk)

        opprettTimeregistrering(oppdragId = oppdragId, dato = "2026-06-15")
        opprettTimeregistrering(oppdragId = oppdrag2Id, dato = "2027-06-15")

        val response = mockMvc.perform(get("/api/timeregistreringer?oppdragId=$oppdragId"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val array = objectMapper.readTree(response)
        assertThat(array.size()).isEqualTo(1)
        assertThat(array[0].get("oppdragId").asLong()).isEqualTo(oppdragId)
    }

    @Test
    fun `skal filtrere timeregistreringer på konsulentId`() {
        // Opprett en andre konsulent med eget oppdrag
        val konsulent2Response = mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Kari",
                        "etternavn": "Nordmann",
                        "epost": "kari@firma.no"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val konsulent2Id = objectMapper.readTree(konsulent2Response).get("id").asLong()

        val oppdrag2Response = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Annet prosjekt",
                        "kundeNavn": "Annen Kunde AS",
                        "startDato": "2026-01-01",
                        "sluttDato": "2026-12-31",
                        "timepris": 1500,
                        "konsulentId": $konsulent2Id
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val oppdrag2Id = objectMapper.readTree(oppdrag2Response).get("id").asLong()

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        ).andExpect(status().isOk)

        opprettTimeregistrering(oppdragId = oppdragId, konsulentId = konsulentId, dato = "2026-06-15")
        opprettTimeregistrering(oppdragId = oppdrag2Id, konsulentId = konsulent2Id, dato = "2026-06-15")

        val response = mockMvc.perform(get("/api/timeregistreringer?konsulentId=$konsulentId"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val array = objectMapper.readTree(response)
        assertThat(array.size()).isEqualTo(1)
        assertThat(array[0].get("konsulentId").asLong()).isEqualTo(konsulentId)
    }

    @Test
    fun `skal filtrere timeregistreringer på både oppdragId og konsulentId`() {
        // Opprett en andre konsulent med eget oppdrag
        val konsulent2Response = mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Per",
                        "etternavn": "Hansen",
                        "epost": "per@firma.no"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val konsulent2Id = objectMapper.readTree(konsulent2Response).get("id").asLong()

        val oppdrag2Response = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Annet prosjekt",
                        "kundeNavn": "Annen Kunde AS",
                        "startDato": "2026-01-01",
                        "sluttDato": "2026-12-31",
                        "timepris": 1500,
                        "konsulentId": $konsulent2Id
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val oppdrag2Id = objectMapper.readTree(oppdrag2Response).get("id").asLong()

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "BEKREFTET" }""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/oppdrag/$oppdrag2Id/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "nyStatus": "AKTIV" }""")
        ).andExpect(status().isOk)

        opprettTimeregistrering(oppdragId = oppdragId, konsulentId = konsulentId, dato = "2026-06-15")
        opprettTimeregistrering(oppdragId = oppdrag2Id, konsulentId = konsulent2Id, dato = "2026-06-16")

        // Filtrer på begge — skal kun returnere treff som matcher begge
        val response = mockMvc.perform(get("/api/timeregistreringer?oppdragId=$oppdragId&konsulentId=$konsulentId"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val array = objectMapper.readTree(response)
        assertThat(array.size()).isEqualTo(1)
        assertThat(array[0].get("oppdragId").asLong()).isEqualTo(oppdragId)
        assertThat(array[0].get("konsulentId").asLong()).isEqualTo(konsulentId)
    }

    @Test
    fun `skal returnere 400 når oppdrag ikke finnes`() {
        mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": 999999,
                        "konsulentId": $konsulentId,
                        "dato": "2026-06-15",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 400 når konsulent ikke finnes`() {
        mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": $oppdragId,
                        "konsulentId": 999999,
                        "dato": "2026-06-15",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 400 når konsulentId ikke matcher oppdragets konsulentId`() {
        // Opprett en andre konsulent
        val konsulent2Response = mockMvc.perform(
            post("/api/konsulenter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "fornavn": "Kari",
                        "etternavn": "Nordmann",
                        "epost": "kari2@firma.no"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val konsulent2Id = objectMapper.readTree(konsulent2Response).get("id").asLong()

        mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": $oppdragId,
                        "konsulentId": $konsulent2Id,
                        "dato": "2026-06-15",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 400 når oppdrag ikke er AKTIV`() {
        // Opprett et nytt oppdrag som IKKE aktiveres
        val inaktivtOppdragResponse = mockMvc.perform(
            post("/api/oppdrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tittel": "Inaktivt prosjekt",
                        "kundeNavn": "Kunde AS",
                        "startDato": "2027-01-01",
                        "sluttDato": "2027-12-31",
                        "timepris": 1000,
                        "konsulentId": $konsulentId
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        val inaktivtOppdragId = objectMapper.readTree(inaktivtOppdragResponse).get("id").asLong()

        mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": $inaktivtOppdragId,
                        "konsulentId": $konsulentId,
                        "dato": "2027-06-15",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `skal returnere 400 ved ugyldig request`() {
        mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "beskrivelse": "" }""")
        )
            .andExpect(status().isBadRequest)
    }

    private fun opprettTimeregistrering(
        oppdragId: Long = this.oppdragId,
        konsulentId: Long = this.konsulentId,
        dato: String = "2026-06-15",
    ): String {
        return mockMvc.perform(
            post("/api/timeregistreringer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "oppdragId": $oppdragId,
                        "konsulentId": $konsulentId,
                        "dato": "$dato",
                        "timer": 7,
                        "minutter": 30,
                        "beskrivelse": "Backend-utvikling"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
    }
}
