package no.decisive.staffhub.felles

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class FeilResponse(
    val melding: String,
    val detaljer: List<String> = emptyList()
)

@RestControllerAdvice
class FeilhåndteringAdvice {

    @ExceptionHandler(IkkeFunnetException::class)
    fun håndterIkkeFunnet(ex: IkkeFunnetException): ResponseEntity<FeilResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(FeilResponse(melding = ex.message ?: "Ressurs ikke funnet"))
    }

    @ExceptionHandler(ValideringException::class)
    fun håndterValidering(ex: ValideringException): ResponseEntity<FeilResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = ex.message ?: "Valideringsfeil"))
    }

    @ExceptionHandler(UgyldigStatusOvergangException::class)
    fun håndterUgyldigStatusOvergang(ex: UgyldigStatusOvergangException): ResponseEntity<FeilResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = ex.message ?: "Ugyldig statusovergang"))
    }

    @ExceptionHandler(OverlappException::class)
    fun håndterOverlapp(ex: OverlappException): ResponseEntity<FeilResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(FeilResponse(melding = ex.message ?: "Overlappende oppdrag"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun håndterValideringsfeil(ex: MethodArgumentNotValidException): ResponseEntity<FeilResponse> {
        val detaljer = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = "Valideringsfeil", detaljer = detaljer))
    }
}
