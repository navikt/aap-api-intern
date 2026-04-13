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

    -- Kun for 'GJELDENDE'
    vedtakstype              text         null check (vedtakstype in ('STANS', 'OPPHØR'))
);
