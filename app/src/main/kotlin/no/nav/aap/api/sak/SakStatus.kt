package no.nav.aap.api.sak

import java.time.LocalDate
import no.nav.aap.api.intern.ArenaStatus as KontraktArenaStatus
import no.nav.aap.api.intern.KelvinStatus as KontraktKelvinStatus
import no.nav.aap.api.intern.NåværendeEnhet as KontraktNåværendeEnhet
import no.nav.aap.api.intern.OppgaveKategori as KontraktOppgaveKategori
import no.nav.aap.api.intern.Periode as KontraktPeriode
import no.nav.aap.api.intern.SakStatus as KontraktSakStatus

sealed interface SakStatus {
    val sakId: String
    val periode: Periode

    data class Arena(
        val statusKode: ArenaStatus,
        override val periode: Periode,
        override val sakId: String,
    ) : SakStatus

    data class Kelvin(
        val statusKode: KelvinStatus,
        override val periode: Periode,
        override val sakId: String,
        val ytelsestatus: YtelseStatus,
        val perioder: List<Periode>,
        val enhet: NåværendeEnhet? = null,
        val forelopigMaksdato: LocalDate? = null,
        val soknadsdatoer: List<LocalDate> = emptyList(),
        val vedtaksdato: LocalDate? = null,
    ) : SakStatus
}

data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
)

data class NåværendeEnhet(
    val oversendtDato: LocalDate,
    val oppgaveKategori: OppgaveKategori,
    val enhet: String,
    val erHasteSak: Boolean,
    val venteAarsak: String?,
)

enum class ArenaStatus {
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,
    UKJENT,
}

enum class KelvinStatus {
    UTREDES,
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}

enum class YtelseStatus {
    FOR_VEDTAK,
    LOPENDE,
    AVSLUTTET,
}

enum class OppgaveKategori {
    MEDLEMSKAP,
    STUDENT,
    LOKALKONTOR,
    KVALITETSSIKRING,
    NAY,
    BESLUTTER,
}

fun SakStatus.tilKontrakt(): KontraktSakStatus =
    when (this) {
        is SakStatus.Arena -> KontraktSakStatus.Arena(
            statusKode = statusKode.tilKontrakt(),
            periode = periode.tilKontrakt(),
            sakId = sakId,
        )

        is SakStatus.Kelvin -> KontraktSakStatus.Kelvin(
            statusKode = statusKode.tilKontrakt(),
            periode = periode.tilKontrakt(),
            sakId = sakId,
            ytelsestatus = ytelsestatus.tilKontrakt(),
            perioder = perioder.map(Periode::tilKontrakt),
            enhet = enhet?.tilKontrakt(),
            forelopigMaksdato = forelopigMaksdato,
            soknadsdatoer = soknadsdatoer,
        )
    }

fun Periode.tilKontrakt(): KontraktPeriode =
    KontraktPeriode(fraOgMedDato, tilOgMedDato)

fun KelvinStatus.tilKontrakt(): KontraktKelvinStatus =
    when (this) {
        KelvinStatus.UTREDES -> KontraktKelvinStatus.UTREDES
        KelvinStatus.SOKNAD_UNDER_BEHANDLING -> KontraktKelvinStatus.SOKNAD_UNDER_BEHANDLING
        KelvinStatus.REVURDERING_UNDER_BEHANDLING -> KontraktKelvinStatus.REVURDERING_UNDER_BEHANDLING
        KelvinStatus.FERDIGBEHANDLET -> KontraktKelvinStatus.FERDIGBEHANDLET
    }

fun ArenaStatus.tilKontrakt(): KontraktArenaStatus =
    when (this) {
        ArenaStatus.AVSLU -> KontraktArenaStatus.AVSLU
        ArenaStatus.FORDE -> KontraktArenaStatus.FORDE
        ArenaStatus.GODKJ -> KontraktArenaStatus.GODKJ
        ArenaStatus.INNST -> KontraktArenaStatus.INNST
        ArenaStatus.IVERK -> KontraktArenaStatus.IVERK
        ArenaStatus.KONT -> KontraktArenaStatus.KONT
        ArenaStatus.MOTAT -> KontraktArenaStatus.MOTAT
        ArenaStatus.OPPRE -> KontraktArenaStatus.OPPRE
        ArenaStatus.REGIS -> KontraktArenaStatus.REGIS
        ArenaStatus.UKJENT -> KontraktArenaStatus.UKJENT
    }

internal fun YtelseStatus.tilKontrakt(): KontraktSakStatus.YtelseStatus =
    when (this) {
        YtelseStatus.FOR_VEDTAK -> KontraktSakStatus.YtelseStatus.FOR_VEDTAK
        YtelseStatus.LOPENDE -> KontraktSakStatus.YtelseStatus.LOPENDE
        YtelseStatus.AVSLUTTET -> KontraktSakStatus.YtelseStatus.AVSLUTTET
    }

private fun NåværendeEnhet.tilKontrakt(): KontraktNåværendeEnhet =
    KontraktNåværendeEnhet(
        oversendtDato = oversendtDato,
        oppgaveKategori = when (oppgaveKategori) {
            OppgaveKategori.MEDLEMSKAP -> KontraktOppgaveKategori.MEDLEMSKAP
            OppgaveKategori.STUDENT -> KontraktOppgaveKategori.STUDENT
            OppgaveKategori.LOKALKONTOR -> KontraktOppgaveKategori.LOKALKONTOR
            OppgaveKategori.KVALITETSSIKRING -> KontraktOppgaveKategori.KVALITETSSIKRING
            OppgaveKategori.NAY -> KontraktOppgaveKategori.NAY
            OppgaveKategori.BESLUTTER -> KontraktOppgaveKategori.BESLUTTER
        },
        enhet = enhet,
        erHasteSak = erHasteSak,
        venteAarsak = venteAarsak,
    )
