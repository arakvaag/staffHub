# StaffHub

Internt oppdragssystem for et IT-konsulentfirma. Bygget med Kotlin og Spring Boot.

## Teknisk stack

- **Språk:** Kotlin
- **Rammeverk:** Spring Boot 4.x
- **Bygging:** Maven
- **Database:** PostgreSQL (Docker)
- **Migrering:** Flyway
- **Dataaksess:** Spring JDBC Template
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Testcontainers

## Arkitektur

Prosjektet følger Domain Driven Design (DDD) med tydelig lagdeling:

```
API-lag            Controller → DTO-mapping → Service
Applikasjonslag    Services med @Transactional
Domenelag          Entiteter, aggregater, enums, exceptions
Infrastrukturlag   Repository (JDBC Template), Tabell-klasser, Flyway
```

## Domenemodell

- **Konsulent** — med kompetanser (fagområde + nivå)
- **Oppdrag** — med livssyklus (FORESLÅTT → BEKREFTET → AKTIV → FULLFØRT/KANSELLERT)
- **Timeregistrering** — timeføring knyttet til aktive oppdrag

## Kjøre lokalt

### Forutsetninger

- Java 25
- Maven
- Docker (for PostgreSQL)

### Start PostgreSQL

```bash
docker run -d \
  --name staffhub-postgres \
  -e POSTGRES_DB=staffhub \
  -e POSTGRES_USER=staffhub \
  -e POSTGRES_PASSWORD=staffhub \
  -p 5432:5432 \
  postgres:16-alpine
```

### Bygg og kjør

```bash
mvn clean install
mvn spring-boot:run
```

### Kjør tester

```bash
mvn test
```

Testene bruker Testcontainers og starter automatisk en PostgreSQL-instans — Docker må kjøre.

## API-endepunkter

| Metode | URL | Beskrivelse |
|--------|-----|-------------|
| POST | /api/konsulenter | Opprett konsulent med kompetanser |
| GET | /api/konsulenter | Hent alle konsulenter (filtrering: ?fagområde=BACKEND) |
| POST | /api/oppdrag | Opprett oppdrag |
| PUT | /api/oppdrag/{id}/status | Endre status på oppdrag |
| POST | /api/timeregistreringer | Registrer timer |
| GET | /api/timeregistreringer | Hent timeregistreringer |
