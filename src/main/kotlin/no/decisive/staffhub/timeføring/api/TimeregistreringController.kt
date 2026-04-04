package no.decisive.staffhub.timeføring.api

import jakarta.validation.Valid
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.timeføring.TimeregistreringService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/timeregistreringer")
class TimeregistreringController(
    private val timeregistreringService: TimeregistreringService,
    private val idProvider: IdProvider,
) {

    @PostMapping
    fun opprett(
        @Valid @RequestBody request: OpprettTimeregistreringRequest
    ): ResponseEntity<TimeregistreringResponse> {
        val timeregistrering = timeregistreringService.opprett(request.tilDomene(idProvider))
        return ResponseEntity.status(HttpStatus.CREATED).body(timeregistrering.tilResponse())
    }

    @GetMapping
    fun hentAlle(
        @RequestParam(required = false) oppdragId: Long?,
        @RequestParam(required = false) konsulentId: Long?,
    ): ResponseEntity<List<TimeregistreringResponse>> {
        val timeregistreringer = when {
            oppdragId != null && konsulentId != null -> timeregistreringService.finnAlleForOppdragOgKonsulent(oppdragId, konsulentId)
            oppdragId != null -> timeregistreringService.finnAlleForOppdrag(oppdragId)
            konsulentId != null -> timeregistreringService.finnAlleForKonsulent(konsulentId)
            else -> timeregistreringService.finnAlle()
        }
        return ResponseEntity.ok(timeregistreringer.map { it.tilResponse() })
    }

    @GetMapping("/{id}")
    fun hentPåId(@PathVariable id: Long): ResponseEntity<TimeregistreringResponse> {
        val timeregistrering = timeregistreringService.hentPåId(id)
        return ResponseEntity.ok(timeregistrering.tilResponse())
    }
}
