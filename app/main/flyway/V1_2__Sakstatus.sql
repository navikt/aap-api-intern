CREATE TABLE SAKER
(
    ID                 BIGSERIAL PRIMARY KEY,
    IDENT              VARCHAR(11)                            NOT NULL,
    SAKSNUMMER         VARCHAR(50)                            NOT NULL,
    OPPRETTET_TID      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    RETTIGHETS_PERIODE DATERANGE                              NOT NULL,
    STATUS             VARCHAR(50)                            NOT NULL
);

CREATE INDEX idx_saker_ident ON saker (IDENT);
CREATE INDEX idx_saker_saksnummer ON saker (SAKSNUMMER);