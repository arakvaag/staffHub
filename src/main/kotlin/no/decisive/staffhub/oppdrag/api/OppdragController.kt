package no.decisive.staffhub.oppdrag.api

import jakarta.validation.Valid
import no.decisive.staffhub.felles.IdProvider
import no.decisive.staffhub.oppdrag.OppdragService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppdrag")
class OppdragController(
    private val oppdragService: OppdragService,
    private val idProvider: IdProvider
) {

    @PostMapping
    fun opprettOppdrag(
        @Valid @RequestBody request: OpprettOppdragRequest
    ): ResponseEntity<OppdragResponse> {
        val oppdrag = oppdragService.opprett(request.tilDomene(idProvider))
        return ResponseEntity.status(HttpStatus.CREATED).body(oppdrag.tilResponse())
    }

    @PutMapping("/{id}/status")
    fun endreStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: EndreStatusRequest
    ): ResponseEntity<OppdragResponse> {
        val oppdrag = oppdragService.endreStatus(id, request.nyStatus)
        return ResponseEntity.ok(oppdrag.tilResponse())
    }
}
