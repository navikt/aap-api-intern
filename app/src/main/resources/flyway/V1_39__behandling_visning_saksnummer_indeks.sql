DROP INDEX IF EXISTS idx_behandling_visning_ident;
ALTER TABLE BEHANDLING_VISNING DROP COLUMN IDENT;
CREATE INDEX idx_behandling_visning_saksnummer ON behandling_visning (SAKSNUMMER);
