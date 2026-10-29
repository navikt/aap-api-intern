package no.nav.aap.api.postgres

import no.nav.aap.api.mineaap.BehandlingHendelseIntern
import no.nav.aap.api.mineaap.BehandlingHendelseNavnIntern
import no.nav.aap.api.mineaap.BehandlingHendelseProgresjonIntern
import no.nav.aap.api.mineaap.BehandlingVisningIntern
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime

class BehandlingVisningRepository(private val connection: DBConnection) {

    fun lagreBehandlingVisning(fnr: String, saksnummer: String, behandlingVisning: BehandlingVisningIntern) {
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
                INSERT INTO BEHANDLING_VISNING_HENDELSE (BEHANDLING_VISNING_ID, NAVN, STATUS, SIST_ENDRET, PROGRESJON)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            behandlingVisning.hendelser
        ) {
            setParams {
                val status: String? = when (it) {
                    is BehandlingHendelseIntern.AlderLovvalgMedlemskap -> it.status?.name
                    is BehandlingHendelseIntern.NavKontor -> it.status?.name
                    is BehandlingHendelseIntern.Nay -> it.status?.name
                    is BehandlingHendelseIntern.SøknadMottatt, is BehandlingHendelseIntern.FerdigBehandlet -> null
                }
                setLong(1, behandlingVisningId)
                setString(2, it.navn.toString())
                setString(3, status)
                setLocalDateTime(4, it.sistEndret)
                setString(5, it.progresjon.toString())
            }
        }
    }

    fun hentBehandlingVisninger(fnr: String): List<BehandlingVisningIntern> {
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
                row.getLong("ID") to BehandlingVisningIntern(
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

    private fun hentHendelser(behandlingVisningId: Long): List<BehandlingHendelseIntern> {
        return connection.queryList(
            """
                SELECT NAVN, STATUS, PROGRESJON, SIST_ENDRET
                FROM BEHANDLING_VISNING_HENDELSE
                WHERE BEHANDLING_VISNING_ID = ?
                ORDER BY ID
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingVisningId)
            }
            setRowMapper { row ->
                tilHendelse(
                    navn = row.getEnum("NAVN"),
                    status = row.getStringOrNull("STATUS"),
                    progresjon = row.getEnum("PROGRESJON"),
                    sistEndret = row.getLocalDateTimeOrNull("SIST_ENDRET"),
                )
            }
        }
    }

    private fun tilHendelse(
        navn: BehandlingHendelseNavnIntern,
        status: String?,
        progresjon: BehandlingHendelseProgresjonIntern,
        sistEndret: LocalDateTime?,
    ): BehandlingHendelseIntern = when (navn) {
        BehandlingHendelseNavnIntern.SØKNAD_MOTTATT -> BehandlingHendelseIntern.SøknadMottatt(
            sistEndret = requireNotNull(sistEndret) { "SIST_ENDRET mangler for SØKNAD_MOTTATT" },
            progresjon = progresjon,
        )

        BehandlingHendelseNavnIntern.ALDER_LOVVALG_MEDLEMSKAP -> BehandlingHendelseIntern.AlderLovvalgMedlemskap(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = status?.let { BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.valueOf(it) },
        )

        BehandlingHendelseNavnIntern.NAV_KONTOR -> BehandlingHendelseIntern.NavKontor(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = status?.let { BehandlingHendelseIntern.NavKontor.Status.valueOf(it) },
        )

        BehandlingHendelseNavnIntern.NAY -> BehandlingHendelseIntern.Nay(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = status?.let { BehandlingHendelseIntern.Nay.Status.valueOf(it) },
        )

        BehandlingHendelseNavnIntern.FERDIG_BEHANDLET -> BehandlingHendelseIntern.FerdigBehandlet(
            sistEndret = sistEndret,
            progresjon = progresjon,
        )
    }
}