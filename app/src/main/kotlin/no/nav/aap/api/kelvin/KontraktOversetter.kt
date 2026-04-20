package no.nav.aap.api.kelvin

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.*
import no.nav.aap.komponenter.type.Periode

fun DatadelingDTO.tilDomene(nyttVedtak: Boolean = false): Behandling {
    return Behandling(
        underveisperiode = this.underveisperiode.map { it.tilDomene() },
        rettighetsperiode = Periode(this.rettighetsPeriodeFom, this.rettighetsPeriodeTom),
        behandlingStatus = this.behandlingStatus.tilDomene(),
        vedtaksDato = this.vedtaksDato,
        sak = this.sak.tilDomene(),
        tilkjent = this.tilkjent.map { it.tilDomene() },
        rettighetsTypePerioder = this.rettighetsTypeTidsLinje.map { it.tilDomene() },
        behandlingsReferanse = this.behandlingsReferanse,
        samId = this.samId,
        vedtakId = this.vedtakId,
        beregningsgrunnlag = this.beregningsgrunnlag,
        nyttVedtak = nyttVedtak,
        stansOpphørVurdering = this.stansOpphørVurdering?.map {
            GjeldendeStansEllerOpphør(
                fom = it.fom,
                opprettet = it.opprettet,
                vurdering = when (it.vurdering) {
                    StansEllerOpphørEnumDTO.STANS -> StansEllerOpphør.STANS
                    StansEllerOpphørEnumDTO.OPPHØR -> StansEllerOpphør.OPPHØR
                },
                avslagsårsaker = it.avslagsårsaker.map { Avslagsårsak.valueOf(it.name) }.toSet()
            )
        }?.toSet().orEmpty()
    )
}

fun SakDTO.tilDomene(): Sak {
    return Sak(
        saksnummer = this.saksnummer,
        status = this.status.tilDomene(),
        opprettetTidspunkt = this.opprettetTidspunkt
    )
}

fun UnderveisDTO.tilDomene(): UnderveisIntern {
    return UnderveisIntern(
        underveisFom = this.underveisFom,
        underveisTom = this.underveisTom,
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

fun TilkjentDTO.tilDomene(): TilkjentPeriode {
    return TilkjentPeriode(
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

fun no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode.tilDomene(): RettighetsTypePeriode {
    return RettighetsTypePeriode(
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

fun ArbeidIPeriodeDTO.tilDomene(): Meldekort.MeldeDag {
    return Meldekort.MeldeDag(
        timerArbeidet = this.timerArbeidet,
        dag = this.periodeFom // antar at periodeFom og periodeTom er samme
    )
}

fun DetaljertMeldekortDTO.tilDomene(): Meldekort {
    return Meldekort(
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
