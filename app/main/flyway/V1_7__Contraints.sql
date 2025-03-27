WITH CTE AS (
    SELECT
        ID,
        ROW_NUMBER() OVER (PARTITION BY SAK_ID ORDER BY OPPRETTET_TID DESC) AS rn
    FROM
        BEHANDLING
)
DELETE FROM BEHANDLING
WHERE ID IN (
    SELECT ID
    FROM CTE
    WHERE rn > 1
);

CREATE UNIQUE INDEX uidx_sak_person_sak_id_person_id ON SAK_PERSON (SAK_ID, PERSON_IDENT);

CREATE UNIQUE INDEX uidx_behandling_sak_id ON BEHANDLING (SAK_ID);

ALTER TABLE rettighetstype
    DROP CONSTRAINT rettighetstype_behandling_id_fkey,
    ADD CONSTRAINT rettighetstype_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (ID) ON DELETE CASCADE;