package no.decisive.staffhub.konsulent.api

import jakarta.validation.Valid
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.konsulent.Kompetanse.Fagområde
import no.decisive.staffhub.konsulent.KonsulentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/konsulenter")
class KonsulentController(
    private val konsulentService: KonsulentService,
    private val idProvider: IdProvider
) {

    @PostMapping
    fun opprettKonsulent(
        @Valid @RequestBody request: OpprettKonsulentRequest
    ): ResponseEntity<KonsulentResponse> {
        val konsulent = konsulentService.opprett(request.tilDomene(idProvider))
        return ResponseEntity.status(HttpStatus.CREATED).body(konsulent.tilResponse())
    }

    @GetMapping
    fun hentAlle(
        @RequestParam(required = false) fagområde: Fagområde?
    ): ResponseEntity<List<KonsulentResponse>> {
        val konsulenter = konsulentService.hentAlle(fagområde)
        return ResponseEntity.ok(konsulenter.map { it.tilResponse() })
    }
}
