package no.decisive.staffhub.felles

class IkkeFunnetException(melding: String) : RuntimeException(melding)

class UgyldigStatusOvergangException(melding: String) : RuntimeException(melding)

class ValideringException(melding: String) : RuntimeException(melding)

class OverlappException(melding: String) : RuntimeException(melding)

class OptimistiskLåsingException(melding: String) : RuntimeException(melding)
