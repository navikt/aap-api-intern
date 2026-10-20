package no.nav.aap.api.postgres

import no.nav.aap.api.mineaap.BehandlingHendelse
import no.nav.aap.api.mineaap.BehandlingVisning
import no.nav.aap.komponenter.dbconnect.DBConnection

class BehandlingVisningRepository(private val connection: DBConnection) {

    fun lagreBehandlingVisning(fnr: String, saksnummer: String, behandlingVisning: BehandlingVisning) {
        val behandlingVisningId = connection.queryFirst(
            """
                INSERT INTO BEHANDLING_VISNING (IDENT, SAKSNUMMER, BEHANDLING_REFERANSE, AVSLUTTET, BEHANDLING_TYPE)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (BEHANDLING_REFERANSE) DO UPDATE SET IDENT           = EXCLUDED.IDENT,
                                                                 SAKSNUMMER      = EXCLUDED.SAKSNUMMER,
                                                                 AVSLUTTET       = EXCLUDED.AVSLUTTET,
                                                                 BEHANDLING_TYPE = EXCLUDED.BEHANDLING_TYPE
                RETURNING ID
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
                setString(2, saksnummer)
                setString(3, behandlingVisning.behandlingReferanse)
                setBoolean(4, behandlingVisning.avsluttet)
                setString(5, behandlingVisning.behandlingType.toString())
            }
            setRowMapper { row ->
                row.getLong("ID")
            }
        }
        connection.execute(
            """
                DELETE FROM BEHANDLING_VISNING_HENDELSE
                WHERE BEHANDLING_VISNING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingVisningId)
            }
        }
        connection.executeBatch(
            """
                INSERT INTO BEHANDLING_VISNING_HENDELSE (BEHANDLING_VISNING_ID, NAVN, STATUS, TIDSSTEMPEL)
                VALUES (?, ?, ?, ?)
            """.trimIndent(),
            behandlingVisning.hendelser
        ) {
            setParams {
                setLong(1, behandlingVisningId)
                setString(2, it.navn.toString())
                setString(3, it.status.toString())
                setLocalDateTime(4, it.tidsstempel)
            }
        }
    }

    fun hentBehandlingVisninger(fnr: String): List<BehandlingVisning> {
        return connection.queryList(
            """
                SELECT ID, BEHANDLING_REFERANSE, AVSLUTTET, BEHANDLING_TYPE
                FROM BEHANDLING_VISNING
                WHERE IDENT = ?
                ORDER BY ID
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
            }
            setRowMapper { row ->
                row.getLong("ID") to BehandlingVisning(
                    behandlingReferanse = row.getString("BEHANDLING_REFERANSE"),
                    avsluttet = row.getBoolean("AVSLUTTET"),
                    behandlingType = row.getEnum("BEHANDLING_TYPE"),
                    hendelser = emptyList(),
                )
            }
        }.map { (id, behandlingVisning) ->
            behandlingVisning.copy(hendelser = hentHendelser(id))
        }
    }

    private fun hentHendelser(behandlingVisningId: Long): List<BehandlingHendelse> {
        return connection.queryList(
            """
                SELECT NAVN, STATUS, TIDSSTEMPEL
                FROM BEHANDLING_VISNING_HENDELSE
                WHERE BEHANDLING_VISNING_ID = ?
                ORDER BY TIDSSTEMPEL, ID
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingVisningId)
            }
            setRowMapper { row ->
                BehandlingHendelse(
                    navn = row.getEnum("NAVN"),
                    status = row.getEnum("STATUS"),
                    tidsstempel = row.getLocalDateTime("TIDSSTEMPEL"),
                )
            }
        }
    }
}