CREATE TABLE SAK
(
    ID                BIGSERIAL PRIMARY KEY,
    STATUS            VARCHAR(100) NOT NULL,
    RETTIGHETSPERIODE DATERANGE    NOT NULL,
    SAKSNUMMER        VARCHAR(100) NOT NULL
);

CREATE TABLE SAK_PERSON
(
    ID           BIGSERIAL NOT NULL PRIMARY KEY,
    SAK_ID       BIGINT    NOT NULL REFERENCES SAK (ID),
    PERSON_IDENT BIGINT    NOT NULL
);

CREATE INDEX IDX_SAK_PERSON_PERSON_ID ON SAK_PERSON (PERSON_IDENT);


CREATE TABLE BEHANDLING
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    SAK_ID        BIGINT                                 NOT NULL REFERENCES SAK (ID),
    STATUS        VARCHAR(100)                           NOT NULL,
    TYPE          VARCHAR(100)                           NOT NULL,
    VEDTAKS_DATO  TIMESTAMP(3)                           NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE UNDERVEIS
(
    ID              BIGSERIAL                              NOT NULL PRIMARY KEY,
    PERIODE         DATERANGE                              NOT NULL,
    MELDEPERIODE    DATERANGE                              NOT NULL,
    BEHANDLING_ID   BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    UTFALL          VARCHAR(100)                           NOT NULL,
    RETTIGHETS_TYPE VARCHAR(100)                           NOT NULL,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE TILKJENT_YTELSE
(
    ID            BIGSERIAL NOT NULL PRIMARY KEY,
    BEHANDLING_ID BIGINT    NOT NULL REFERENCES BEHANDLING (ID)
);

CREATE UNIQUE INDEX UIDX_TILKJENT_YTELSE ON TILKJENT_YTELSE (BEHANDLING_ID);

CREATE TABLE TILKJENT_PERIODE
(
    ID                 BIGSERIAL       NOT NULL PRIMARY KEY,
    PERIODE            DATERANGE       NOT NULL,
    DAGSATS            NUMERIC(21, 0)  NOT NULL,
    GRUNNLAG           NUMERIC(21, 0)  NOT NULL,
    GRADERING          SMALLINT        NOT NULL,
    GRUNNBELOP         NUMERIC(21)     NOT NULL,
    ANTALL_BARN        SMALLINT        NOT NULL,
    BARNETILLEGG       NUMERIC(21)     NOT NULL,
    GRUNNLAGSFAKTOR    NUMERIC(21, 10) NOT NULL,
    BARNETILLEGGSATS   NUMERIC         NOT NULL,
    TILKJENT_YTELSE_ID BIGINT          NOT NULL REFERENCES TILKJENT_YTELSE (ID)
);
