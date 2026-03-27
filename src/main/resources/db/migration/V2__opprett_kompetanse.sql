CREATE SEQUENCE kompetanse_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE kompetanse
(
    id           BIGINT PRIMARY KEY,
    konsulent_id BIGINT REFERENCES konsulent (id),
    fagomrade    VARCHAR(50),
    niva         VARCHAR(50),
    beskrivelse  TEXT
);
