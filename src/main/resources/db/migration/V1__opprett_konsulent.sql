CREATE SEQUENCE konsulent_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE konsulent
(
    id             BIGINT PRIMARY KEY,
    fornavn        VARCHAR(255),
    etternavn      VARCHAR(255),
    epost          VARCHAR(255) UNIQUE,
    telefon        VARCHAR(50),
    opprettet_dato TIMESTAMP
);
