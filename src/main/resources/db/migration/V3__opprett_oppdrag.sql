CREATE SEQUENCE oppdrag_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE oppdrag
(
    id             BIGINT PRIMARY KEY,
    tittel         VARCHAR(255),
    kunde_navn     VARCHAR(255),
    beskrivelse    TEXT,
    start_dato     DATE,
    slutt_dato     DATE,
    status         VARCHAR(50),
    timepris       DECIMAL(10, 2),
    konsulent_id   BIGINT REFERENCES konsulent (id),
    opprettet_dato TIMESTAMP
);
