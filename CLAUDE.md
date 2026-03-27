# StaffHub

Internt oppdragssystem for IT-konsulentfirma. Kotlin + Spring Boot 4 + PostgreSQL.

## Teknisk stack

- **Kotlin 2.1.20** på JVM 23 (Kotlin støtter ikke høyere JVM-target ennå)
- **Spring Boot 4.0.5** med Maven
- **PostgreSQL** med Flyway-migrasjoner, Spring JDBC Template (ingen JPA/Hibernate)
- **Jackson 3** (`tools.jackson` groupId, ikke `com.fasterxml.jackson`)
- **Testcontainers 2.0.4** (`org.testcontainers.postgresql.PostgreSQLContainer`)

## Språk og navngivning

- Domenebegreper bruker **norsk**: Konsulent, Oppdrag, Kompetanse, Timeregistrering, Fagområde, osv.
- Tekniske navn bruker **engelsk**: Repository, Controller, Service, insert, select, osv.
- API-endepunkter bruker norsk: `/api/konsulenter`, `/api/oppdrag`

## Arkitektur

Hver modul (konsulent, oppdrag, timeføring) er organisert slik:

```
konsulent/
  Konsulent.kt, Kompetanse.kt    # Domenemodell (aggregat)
  KonsulentService.kt             # Forretningslogikk
  api/
    KonsulentController.kt        # REST-endepunkt
    KonsulentDto.kt               # Request/Response-DTOer med tilDomene()/tilResponse()
  persistering/
    KonsulentRepository.kt        # Lagrer/henter hele aggregater
    KonsulentTabell.kt            # JDBC-operasjoner mot enkelt-tabeller
```

### Domeneklasser

- **Private konstruktører** — kun to lovlige måter å opprette på:
  1. `Konsulent(idProvider, ...)` — for nye objekter, henter ID fra DB-sekvens
  2. `Konsulent.fra(PersistertState)` — for gjenskapning fra database, markerer automatisk som persistert
- **Dirty tracking** via snapshot-mønster: `erNy`, `erEndret`, `bekreftPersistert()`
- `PersistertState` er en public data class som brukes både som factory-input og som intern snapshot
- Enums `Fagområde` og `Kompetansenivå` er nestet i `Kompetanse`-klassen

### Tabell-klasser

- Speiler DB-tabeller direkte med nullable felter
- Bruker `JdbcTemplate` og `RowMapper`
- Ingen domenelogikk — bare SQL insert/update/select

### Repository

- Concrete classes (ingen interfaces) — Mockito kan mocke dem direkte
- Mapper mellom domene og tabell-rader
- Kaller `bekreftPersistert()` etter lagring
- Bygger `PersistertState` og kaller `Entitet.fra(state)` ved henting

### IdProvider

Spring-komponent som kaller `SELECT nextval('sekvens')` for å hente neste ID. Injiseres i domene-konstruktører.

## Teststrategi

Tre lag med tester, persisterings- og komponenttester krever Docker (for Testcontainers):

1. **Unit-tester** (domeneklasser, service med mockede repos): Ingen Spring-kontekst, raske
2. **Persisteringstester** (`DatabaseTest`-baseklasse): Testcontainers + Flyway + JdbcTemplate direkte, ingen Spring-kontekst. Kjører `flyway.clean()` + `migrate()` før hver test
3. **Komponenttester** (`@SpringBootTest` + `@AutoConfigureMockMvc`): Full Spring-kontekst med Testcontainers

Kjør alle tester: `mvn clean test`

## Moduler

- **konsulent** — Komplett: domene, API, persistering, tester
- **oppdrag** — Komplett: domene med statusmaskin, API, persistering, tester
- **timeføring** — Kun domeneklasse (`Timeregistrering`). Ment som workshop-oppgave — mangler DTO, controller, service, repository, tabell, tester

## Viktige designvalg

- Ikke bruk interfaces for repositories — konkrete klasser holder
- Ikke bruk `jacksonObjectMapper()` eller andre Jackson 2-APIer
- Spring Boot 4 har flyttet `AutoConfigureMockMvc` til `org.springframework.boot.webmvc.test.autoconfigure`
- Flyway krever `spring-boot-starter-flyway` (ikke bare `flyway-core`) i Spring Boot 4
