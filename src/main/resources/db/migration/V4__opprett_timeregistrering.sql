CREATE SEQUENCE timeregistrering_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE timeregistrering
(
    id             BIGINT PRIMARY KEY,
    oppdrag_id     BIGINT REFERENCES oppdrag (id),
    konsulent_id   BIGINT REFERENCES konsulent (id),
    dato           DATE,
    timer          INTEGER,
    minutter       INTEGER,
    beskrivelse    TEXT,
    opprettet_dato TIMESTAMP
);
