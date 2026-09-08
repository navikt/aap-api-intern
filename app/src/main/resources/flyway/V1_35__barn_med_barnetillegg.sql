CREATE TABLE BARN_MED_BARNETILLEGG
(
    id            BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT    NOT NULL REFERENCES BEHANDLING (ID),
    ident         TEXT,
    periode       DATERANGE NOT NULL,
    belop         NUMERIC   NOT NULL
);

CREATE INDEX ON BARN_MED_BARNETILLEGG (behandling_id);
