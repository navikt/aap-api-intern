CREATE TABLE ARENAVEDTAK
(
    id             BIGSERIAL PRIMARY KEY,
    behandling_id  BIGSERIAL NOT NULL,
    vedtak_id      BIGINT    NOT NULL,
    vedtaksvariant TEXT      NOT NULL,
    fom            DATE      NOT NULL,
    tom            DATE      NOT NULL
);
