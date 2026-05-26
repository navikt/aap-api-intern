create table stans_opphor_grunnlag
(
    id            bigserial primary key,
    behandling_id bigint       not null references behandling (id) on delete cascade,
    opprettet_tid timestamp(3) not null
);

create table stans_opphor_vurdering
(
    id                       bigserial primary key,
    stans_opphor_grunnlag_id bigint       not null references stans_opphor_grunnlag (id) on delete cascade,
    opprettet_tid            timestamp(3) not null,

    fom                      date         not null,

    vedtakstype              text         not null check (vedtakstype in ('STANS', 'OPPHØR'))
);

CREATE INDEX IDX_STANS_OPPHOR_GRUNNLAG_BEHANDLING_ID ON stans_opphor_grunnlag (behandling_id);
CREATE INDEX IDX_STANS_OPPHOR_VURDERING_GRUNNLAG_ID ON stans_opphor_vurdering (stans_opphor_grunnlag_id);
