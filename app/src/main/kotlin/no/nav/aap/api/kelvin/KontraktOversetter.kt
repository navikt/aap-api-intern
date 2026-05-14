package no.nav.aap.api.kelvin

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenaVedtaksvariantDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.StansEllerOpphørEnumDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

fun DatadelingDTO.tilDomene(nyttVedtak: Boolean = false): Behandling {
    return Behandling(
        rettighetsperiode = Periode(this.rettighetsPeriodeFom, this.rettighetsPeriodeTom),
        behandlingStatus = this.behandlingStatus.tilDomene(),
        vedtaksDato = this.vedtaksDato,
        sak = this.sak.tilDomene(),
        tilkjent = this.tilkjent.somTidslinje(
            { Periode(it.tilkjentFom, it.tilkjentTom) },
            { it.tilDomene() }),
        rettighetsTypePerioder = this.rettighetsTypeTidsLinje.map { it.tilDomene() },
        behandlingsReferanse = this.behandlingsReferanse,
        samId = this.samId,
        vedtakId = this.vedtakId,
        beregningsgrunnlag = this.beregningsgrunnlag,
        nyttVedtak = nyttVedtak,
        stansOpphørVurdering = this.stansOpphørVurdering?.map { stansEllerOpphør ->
            GjeldendeStansEllerOpphør(
                fom = stansEllerOpphør.fom,
                opprettet = stansEllerOpphør.opprettet,
                vurdering = when (stansEllerOpphør.vurdering) {
                    StansEllerOpphørEnumDTO.STANS -> StansEllerOpphør.STANS
                    StansEllerOpphørEnumDTO.OPPHØR -> StansEllerOpphør.OPPHØR
                },
                avslagsårsaker = stansEllerOpphør.avslagsårsaker.map { Avslagsårsak.valueOf(it.name) }
                    .toSet()
            )
        }?.toSet().orEmpty(),
        arenakompatibleVedtak = this.arenavedtak.map {
            Arenavedtak(
                vedtakId = it.vedtakId,
                fom = it.fom,
                tom = it.tom,
                vedtaksvariant = when (it.vedtaksvariant) {
                    ArenaVedtaksvariantDTO.O_AVSLAG -> Arenavedtak.Vedtaksvariant.O_AVSLAG
                    ArenaVedtaksvariantDTO.O_INNV_NAV -> Arenavedtak.Vedtaksvariant.O_INNV_NAV
                    ArenaVedtaksvariantDTO.O_INNV_SOKNAD -> Arenavedtak.Vedtaksvariant.O_INNV_SOKNAD
                    ArenaVedtaksvariantDTO.E_FORLENGE -> Arenavedtak.Vedtaksvariant.E_FORLENGE
                    ArenaVedtaksvariantDTO.E_VERDI -> Arenavedtak.Vedtaksvariant.E_VERDI
                    ArenaVedtaksvariantDTO.G_AVSLAG -> Arenavedtak.Vedtaksvariant.G_AVSLAG
                    ArenaVedtaksvariantDTO.G_INNV_NAV -> Arenavedtak.Vedtaksvariant.G_INNV_NAV
                    ArenaVedtaksvariantDTO.G_INNV_SOKNAD -> Arenavedtak.Vedtaksvariant.G_INNV_SOKNAD
                    ArenaVedtaksvariantDTO.S_DOD -> Arenavedtak.Vedtaksvariant.S_DOD
                    ArenaVedtaksvariantDTO.S_OPPHOR -> Arenavedtak.Vedtaksvariant.S_OPPHOR
                    ArenaVedtaksvariantDTO.S_STANS -> Arenavedtak.Vedtaksvariant.S_STANS
                },
            )
        },
    )
}

fun SakDTO.tilDomene(): Sak {
    return Sak(
        saksnummer = this.saksnummer,
        opprettetTidspunkt = this.opprettetTidspunkt
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

fun TilkjentDTO.tilDomene(): TilkjentYtelse {
    return TilkjentYtelse(
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
    )
}
