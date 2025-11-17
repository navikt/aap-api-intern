package no.nav.aap.api.kelvin

import no.nav.aap.api.postgres.KelvinBehandlingStatus
import no.nav.aap.api.postgres.KelvinSakStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.*
import no.nav.aap.komponenter.type.Periode

fun DatadelingDTO.tilDomene(nyttVedtak: Boolean = false): no.nav.aap.api.postgres.DatadelingDTO {
    return no.nav.aap.api.postgres.DatadelingDTO(
        underveisperiode = this.underveisperiode.map { it.tilDomene() },
        rettighetsPeriodeFom = this.rettighetsPeriodeFom,
        rettighetsPeriodeTom = this.rettighetsPeriodeTom,
        behandlingStatus = this.behandlingStatus.tilDomene(),
        behandlingsId = this.behandlingsId,
        vedtaksDato = this.vedtaksDato,
        sak = this.sak.tilDomene(),
        tilkjent = this.tilkjent.map { it.tilDomene() },
        rettighetsTypeTidsLinje = this.rettighetsTypeTidsLinje.map { it.tilDomene() },
        behandlingsReferanse = this.behandlingsReferanse,
        samId = this.samId,
        vedtakId = this.vedtakId,
        beregningsgrunnlag = this.beregningsgrunnlag,
        nyttVedtak = nyttVedtak
    )
}

fun SakDTO.tilDomene(): no.nav.aap.api.postgres.SakDTO {
    return no.nav.aap.api.postgres.SakDTO(
        saksnummer = this.saksnummer,
        status = this.status.tilDomene(),
        fnr = this.fnr,
        opprettetTidspunkt = this.opprettetTidspunkt
    )
}

fun UnderveisDTO.tilDomene(): no.nav.aap.api.postgres.UnderveisDTO {
    return no.nav.aap.api.postgres.UnderveisDTO(
        underveisFom = this.underveisFom,
        underveisTom = this.underveisFom,
        meldeperiodeFom = this.meldeperiodeFom,
        meldeperiodeTom = this.meldeperiodeTom,
        utfall = this.utfall,
        rettighetsType = this.rettighetsType,
        avslagsårsak = this.avslagsårsak
    )
}

fun Status.tilDomene(): KelvinBehandlingStatus {
    return when (this) {
        Status.OPPRETTET -> KelvinBehandlingStatus.OPPRETTET
        Status.UTREDES -> KelvinBehandlingStatus.UTREDES
        Status.IVERKSETTES -> KelvinBehandlingStatus.IVERKSETTES
        Status.AVSLUTTET -> KelvinBehandlingStatus.AVSLUTTET
    }
}

fun TilkjentDTO.tilDomene(): no.nav.aap.api.postgres.TilkjentDTO {
    return no.nav.aap.api.postgres.TilkjentDTO(
        tilkjentFom = this.tilkjentFom,
        tilkjentTom = this.tilkjentTom,
        dagsats = this.dagsats,
        gradering = this.gradering,
        samordningUføregradering = this.samordningUføregradering,
        grunnlagsfaktor = this.grunnlagsfaktor,
        grunnbeløp = this.grunnbeløp,
        antallBarn = this.antallBarn,
        barnetilleggsats = this.barnetilleggsats,
        barnetillegg = this.barnetillegg
    )
}

fun RettighetsTypePeriode.tilDomene(): no.nav.aap.api.postgres.RettighetsTypePeriode {
    return no.nav.aap.api.postgres.RettighetsTypePeriode(
        fom = this.fom,
        tom = this.tom,
        verdi = this.verdi
    )
}

fun no.nav.aap.behandlingsflyt.kontrakt.sak.Status.tilDomene(): KelvinSakStatus {
    return when (this) {
        no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET -> KelvinSakStatus.OPPRETTET
        no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES -> KelvinSakStatus.UTREDES
        no.nav.aap.behandlingsflyt.kontrakt.sak.Status.LØPENDE -> KelvinSakStatus.LØPENDE
        no.nav.aap.behandlingsflyt.kontrakt.sak.Status.AVSLUTTET -> KelvinSakStatus.AVSLUTTET
    }
}

fun ArbeidIPeriodeDTO.tilDomene(): MeldekortDTO.MeldeDag {
    return MeldekortDTO.MeldeDag(
        timerArbeidet = this.timerArbeidet,
        dag = this.periodeFom // antar at periodeFom og periodeTom er samme
    )
}

fun DetaljertMeldekortDTO.tilDomene(): MeldekortDTO {
    return MeldekortDTO(
        personIdent = this.personIdent,
        saksnummer = this.saksnummer.toString(),
        behandlingId = this.behandlingId,
        mottattTidspunkt = this.mottattTidspunkt,
        meldePeriode = Periode(this.meldeperiodeFom, this.meldeperiodeTom),
        arbeidPerDag = this.timerArbeidPerPeriode.map { it.tilDomene() },
        meldepliktStatusKode = this.meldepliktStatusKode,
        rettighetsTypeKode = this.rettighetsTypeKode,
        avslagsårsakKode = this.avslagsårsakKode
    )
}
