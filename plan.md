# Timeregistrering-modulen: TDD-implementasjonsplan

## Kontekst

Timeregistrering er den tredje modulen i StaffHub. Domenemodellen (`Timeregistrering.kt`) og databasemigrasjonen (`V4__opprett_timeregistrering.sql`) finnes allerede. Det som mangler er DTO-er, tabell-klasse, repository, service og controller — pluss tester på alle tre nivåer.

**API-endepunkter:**
- `POST /api/timeregistreringer` — Opprett ny timeregistrering (201)
- `GET /api/timeregistreringer` — Hent alle, med valgfrie filtre `?oppdragId=X` og `?konsulentId=X`
- `GET /api/timeregistreringer/{id}` — Hent én på ID (200 / 404)

**Forretningsregler i service:**
1. Oppdrag må finnes (ellers `ValideringException`)
2. Konsulent må finnes (ellers `ValideringException`)
3. `konsulentId` på timeregistreringen må matche `konsulentId` på oppdraget
4. Oppdraget må ha status `AKTIV`

---

## Filer som skal opprettes

### Produksjonskode
| Fil | Beskrivelse |
|-----|-------------|
| `src/main/kotlin/no/decisive/staffhub/timeføring/persistering/TimeregistreringTabell.kt` | `TimeregistreringRad` + JDBC-operasjoner |
| `src/main/kotlin/no/decisive/staffhub/timeføring/persistering/TimeregistreringRepository.kt` | Mapper mellom domene og tabell |
| `src/main/kotlin/no/decisive/staffhub/timeføring/TimeregistreringService.kt` | Forretningslogikk og validering |
| `src/main/kotlin/no/decisive/staffhub/timeføring/api/TimeregistreringDto.kt` | Request/Response-DTO-er med `tilDomene()`/`tilResponse()` |
| `src/main/kotlin/no/decisive/staffhub/timeføring/api/TimeregistreringController.kt` | REST-endepunkter |

### Tester
| Fil | Beskrivelse |
|-----|-------------|
| `src/test/kotlin/no/decisive/staffhub/komponenttest/TimeregistreringApiKomponentTest.kt` | Black-box API-tester |
| `src/test/kotlin/no/decisive/staffhub/timeføring/TimeregistreringServiceTest.kt` | Unit-tester med mockede repos |
| `src/test/kotlin/no/decisive/staffhub/persistering/TimeregistreringRepositoryTest.kt` | Integrasjonstester mot DB |

---

## TDD-steg

Planen følger strict outside-in TDD. Det er normalt at komponenttesten er rød mens indre lag bygges.

### Fase 1: POST happy path — komponenttest driver alt

#### Steg 1 — RØD komponenttest: happy path POST
Opprett `TimeregistreringApiKomponentTest`. `@BeforeEach` oppretter en konsulent via API, deretter et oppdrag som aktiveres (FORESLÅTT → BEKREFTET → AKTIV). Test: POST `/api/timeregistreringer` med gyldig JSON, forvent 201 og korrekt respons-body (JSONAssert LENIENT).

Feiler med 404 — ingen controller. La den stå rød.

#### Steg 2 — RØD persisteringstest: lagre og hente
Opprett `TimeregistreringRepositoryTest` (extends `DatabaseTest`). `@BeforeEach` setter opp `IdProvider`, alle nødvendige tabeller/repositories, og oppretter testkonsulent + testoppdrag. Test: lagre en `Timeregistrering`, hent den med `hentPåId`, assert alle felter.

Feiler — `TimeregistreringTabell` og `TimeregistreringRepository` finnes ikke.

#### Steg 3 — GRØNN: opprett `TimeregistreringTabell`
- `data class TimeregistreringRad(id, oppdragId, konsulentId, dato, timer, minutter, beskrivelse, opprettetDato)` — nullable felter
- `@Component class TimeregistreringTabell(jdbc)` med `radMapper`, `insert()`, `selectById()`, `selectAll()`, `selectByOppdragId()`, `selectByKonsulentId()`
- Følg `OppdragTabell`-mønsteret nøyaktig

#### Steg 4 — GRØNN: opprett `TimeregistreringRepository`
- `@Repository class TimeregistreringRepository(timeregistreringTabell)`
- `lagre()`: sjekk `erNy`, kall `insert(tilRad(...))`, kall `bekreftPersistert()`
- `hentPåId()`: kast `IkkeFunnetException` hvis ikke funnet
- `finnAlle()`, `finnAlleForOppdrag(oppdragId)`, `finnAlleForKonsulent(konsulentId)`
- Private `tilRad()` og `tilDomene()` (bruker `Timeregistrering.fra(PersistertState(...))`)

Persisteringstesten fra steg 2 blir grønn.

#### Steg 5 — RØD unit-test: opprett happy path
Opprett `TimeregistreringServiceTest`. Mock `TimeregistreringRepository`, `OppdragRepository`, `KonsulentRepository`. Test: stub oppdrag (AKTIV, matchende konsulentId), stub konsulent, kall `service.opprett(...)`, verify `lagre` kalles.

Feiler — `TimeregistreringService` finnes ikke.

#### Steg 6 — GRØNN: opprett `TimeregistreringService`
- `@Service @Transactional class TimeregistreringService(timeregistreringRepository, oppdragRepository, konsulentRepository)`
- `opprett()`: valider oppdrag finnes → valider oppdrag er AKTIV → valider konsulent finnes → valider konsulentId matcher → lagre
- `hentPåId()`, `finnAlle()`, `finnAlleForOppdrag()`, `finnAlleForKonsulent()` — delegerer til repository, `@Transactional(readOnly = true)`

Unit-test fra steg 5 blir grønn.

#### Steg 7 — Opprett DTO-er
- `OpprettTimeregistreringRequest` med `@field:NotNull` på `oppdragId`, `konsulentId`, `dato`, `timer`, `minutter` og `@field:NotBlank` på `beskrivelse`
- `TimeregistreringResponse` med alle felter inkl. `totalMinutter`
- Extensionfunksjoner `tilDomene(idProvider)` og `tilResponse()`

#### Steg 8 — GRØNN: opprett `TimeregistreringController`
- `@RestController @RequestMapping("/api/timeregistreringer")`
- `@PostMapping` → `request.tilDomene(idProvider)` → `service.opprett(...)` → `201 CREATED`
- `@GetMapping` med `@RequestParam(required = false) oppdragId` og `konsulentId` → dispatch til riktig service-metode → `200 OK`
- `@GetMapping("/{id}")` → `service.hentPåId(id)` → `200 OK`

Komponenttesten fra steg 1 blir grønn.

#### Steg 9 — REFAKTORER
Gjennomgå all ny kode for konsistens, navngivning, imports.

---

### Fase 2: GET-endepunkter — komponenttester

#### Steg 10 — Komponenttest: hent på ID
`skal hente timeregistrering på id` — POST, hent ut ID, GET `/api/timeregistreringer/{id}`, forvent 200 med korrekt body.

#### Steg 11 — Komponenttest: 404 for ukjent ID
`skal returnere 404 for ukjent timeregistrering` — GET `/api/timeregistreringer/999999`, forvent 404.

#### Steg 12 — Komponenttest: hent alle
`skal hente alle timeregistreringer` — POST to timeregistreringer, GET `/api/timeregistreringer`, forvent JSON-array med 2 elementer.

#### Steg 13 — Komponenttest: filtrer på oppdragId
`skal filtrere timeregistreringer på oppdragId` — Opprett to oppdrag (begge AKTIV, ikke-overlappende datoer), POST én timeregistrering på hvert, GET med `?oppdragId={id}`, forvent kun 1 resultat.

#### Steg 14 — Komponenttest: filtrer på konsulentId
`skal filtrere timeregistreringer på konsulentId` — Opprett to konsulenter med hvert sitt oppdrag, POST én timeregistrering per konsulent, GET med `?konsulentId={id}`, forvent kun 1 resultat.

Disse bør bli grønne umiddelbart siden controller ble implementert i steg 8.

---

### Fase 3: Valideringsfeil — unit-tester driver, komponenttester bekrefter

#### Steg 15 — Unit-test: oppdrag finnes ikke
`skal feile når oppdrag ikke finnes` — stub `oppdragRepository.hentPåId` til å kaste `IkkeFunnetException`. Assert `ValideringException`. Verify `lagre` aldri kalt.

#### Steg 16 — Unit-test: konsulent finnes ikke
`skal feile når konsulent ikke finnes` — oppdrag finnes (AKTIV), men konsulent kaster `IkkeFunnetException`. Assert `ValideringException`.

#### Steg 17 — Unit-test: konsulentId matcher ikke
`skal feile når konsulentId ikke matcher oppdragets konsulentId` — oppdrag har annen konsulentId. Assert `ValideringException`.

#### Steg 18 — Unit-test: oppdrag ikke AKTIV
`skal feile når oppdrag ikke er AKTIV` — oppdrag har status `FORESLÅTT`. Assert `ValideringException`.

#### Steg 19 — Komponenttest: POST med ukjent oppdragId → 400
#### Steg 20 — Komponenttest: POST med ukjent konsulentId → 400
#### Steg 21 — Komponenttest: POST med feil konsulentId → 400
#### Steg 22 — Komponenttest: POST med ikke-AKTIV oppdrag → 400
#### Steg 23 — Komponenttest: POST med ugyldig request → 400

---

### Fase 4: Repository-edge-cases

#### Steg 24 — `skal kaste IkkeFunnetException for ukjent id`
#### Steg 25 — `skal finne alle timeregistreringer`
#### Steg 26 — `skal finne timeregistreringer for oppdrag`
#### Steg 27 — `skal finne timeregistreringer for konsulent`
#### Steg 28 — `lagret timeregistrering skal ikke lenger være markert som ny` + `hentet skal ikke være ny eller endret`
#### Steg 29 — `finnAlle skal returnere tom liste når ingen finnes`

---

### Fase 5: Sluttrefaktorering

#### Steg 30 — REFAKTORER
Gjennomgå feilmeldinger (norsk, konsistent stil), controller-logikk for filtre, kjør hele testsuiten: `mvn clean test`.

---

## Eksisterende filer som gjenbrukes

| Fil | Hva gjenbrukes |
|-----|----------------|
| `src/main/kotlin/no/decisive/staffhub/timeføring/Timeregistrering.kt` | Domenemodell (ferdig) |
| `src/main/resources/db/migration/V4__opprett_timeregistrering.sql` | DB-migrasjon (ferdig) |
| `src/main/kotlin/no/decisive/staffhub/felles/IdProvider.kt` | `nesteTimeregistreringId()` (ferdig) |
| `src/main/kotlin/no/decisive/staffhub/felles/Exceptions.kt` | `IkkeFunnetException`, `ValideringException` |
| `src/main/kotlin/no/decisive/staffhub/felles/FeilhåndteringAdvice.kt` | Global exception → HTTP-mapping (ferdig) |
| `src/main/kotlin/no/decisive/staffhub/oppdrag/persistering/OppdragRepository.kt` | Injiseres i service for validering |
| `src/main/kotlin/no/decisive/staffhub/konsulent/persistering/KonsulentRepository.kt` | Injiseres i service for validering |
| `src/test/kotlin/no/decisive/staffhub/persistering/DatabaseTest.kt` | Baseklasse for persisteringstester |
| `src/test/kotlin/no/decisive/staffhub/komponenttest/KomponentTest.kt` | Baseklasse for komponenttester |

## Verifisering

Kjør hele testsuiten etter hvert steg som skal være grønt:
```bash
mvn clean test
```

For å kjøre kun én testklasse underveis:
```bash
mvn test -Dtest=TimeregistreringApiKomponentTest
mvn test -Dtest=TimeregistreringServiceTest
mvn test -Dtest=TimeregistreringRepositoryTest
```
