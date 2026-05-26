CREATE TABLE avslagsarsak
(
    id                        BIGSERIAL PRIMARY KEY,
    stans_opphor_vurdering_id BIGINT NOT NULL REFERENCES stans_opphor_vurdering (id) on delete cascade,
    avslagsarsak              TEXT   NOT NULL
);

CREATE INDEX IDX_STANS_OPPHOR_VURDERING_AVSLAGSARSAK_VURDERING_ID ON avslagsarsak (stans_opphor_vurdering_id);