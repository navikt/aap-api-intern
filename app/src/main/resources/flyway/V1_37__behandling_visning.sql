CREATE TABLE BEHANDLING_VISNING
(
    ID                      BIGSERIAL       PRIMARY KEY,
    IDENT                   VARCHAR(11)     NOT NULL,
    SAKSNUMMER              VARCHAR(50)     NOT NULL,
    BEHANDLING_REFERANSE    VARCHAR(100)    NOT NULL,
    AVSLUTTET               BOOLEAN         NOT NULL,
    BEHANDLING_TYPE         VARCHAR(50)     NOT NULL
);

CREATE INDEX idx_behandling_visning_ident ON behandling_visning (IDENT);
CREATE UNIQUE INDEX idx_behandling_visning_behandling_referanse ON behandling_visning (BEHANDLING_REFERANSE);

CREATE TABLE BEHANDLING_VISNING_HENDELSE
(
    ID                      BIGSERIAL       PRIMARY KEY,
    BEHANDLING_VISNING_ID   BIGINT          NOT NULL REFERENCES BEHANDLING_VISNING (ID) ON DELETE CASCADE,
    NAVN                    VARCHAR(75)     NOT NULL,
    STATUS                  VARCHAR(50)     NOT NULL,
    TIDSSTEMPEL             TIMESTAMP(3)    NOT NULL
);

CREATE INDEX idx_behandling_visning_hendelse_behandling_visning_id ON behandling_visning_hendelse (BEHANDLING_VISNING_ID);
