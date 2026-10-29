package no.nav.aap.api.kelvin

import no.nav.aap.api.mineaap.BehandlingHendelseIntern
import no.nav.aap.api.mineaap.BehandlingHendelseProgresjonIntern
import no.nav.aap.api.mineaap.BehandlingVisningIntern
import no.nav.aap.api.mineaap.BehandlingVisningTypeIntern
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenaVedtaksvariantDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BarnMedBarnetilleggDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseProgresjon
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseType
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingVisningDto
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.PeriodeMedBeløpDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.StansEllerOpphørEnumDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisperiodeDatadelingDTO
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
        samIdOgTpNr = this.samIdOgTpr.map { SamIdOgTpnr(it.samId, it.tpnr) },
        vedtakId = this.vedtakId,
        beregningsgrunnlag = this.beregningsgrunnlag,
        nyttVedtak = nyttVedtak,
        foreløpigMaksdato = this.muligMaksdato,
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
        perioderMedFritakMeldeplikt = this.perioderMedFritakMeldeplikt.map { Periode(it.fom, it.tom) },
        underveisperioder = this.underveisperioder.map { it.tilDomene() },
        barnMedBarnetillegg = this.barnMedBarnetillegg.map { it.tilDomene() },
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
        effektivDagsats = this.effektivDagsats,
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
        dag = this.periodeFom,
    )
}

fun UnderveisperiodeDatadelingDTO.tilDomene(): Underveisperiode {
    return Underveisperiode(
        periode = Periode(this.periode.fom, this.periode.tom),
        meldeperiode = Periode(this.meldeperiode.fom, this.meldeperiode.tom),
        meldepliktstatus = this.meldepliktstatus,
        arbeidsgrad = this.arbeidsgrad,
        overgrenseVerdi = this.overgrenseVerdi,
        timerArbeidet = this.timerArbeidet,
    )
}

fun DetaljertMeldekortDTO.tilDomene(): Meldekort {
    return Meldekort(
        personIdent = this.personIdent,
        saksnummer = this.saksnummer.toString(),
        behandlingId = this.behandlingId,
        journalpostId = this.journalpostId,
        mottattTidspunkt = this.mottattTidspunkt,
        meldePeriode = Periode(this.meldeperiodeFom, this.meldeperiodeTom),
        arbeidPerDag = this.timerArbeidPerPeriode.map { it.tilDomene() },
    )
}

fun BarnMedBarnetilleggDTO.tilDomene(): BarnMedBarnetillegg {
    return BarnMedBarnetillegg(
        ident = this.ident,
        perioderMedBarnetillegg = this.perioderMedBarnetillegg.map { it.tilDomene() },
    )
}

fun PeriodeMedBeløpDTO.tilDomene(): PeriodeMedBeløp {
    return PeriodeMedBeløp(
        fom = this.fom,
        tom = this.tom,
        beløp = this.beløp,
        sats = this.sats,
        uredusertBeløp = this.uredusertBeløp
    )
}
fun BehandlingHendelseProgresjon.tilDomene(): BehandlingHendelseProgresjonIntern {
    return when(this) {
        BehandlingHendelseProgresjon.AKTIV -> BehandlingHendelseProgresjonIntern.AKTIV
        BehandlingHendelseProgresjon.IKKE_PÅBEGYNT -> BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT
        BehandlingHendelseProgresjon.FULLFØRT -> BehandlingHendelseProgresjonIntern.FULLFØRT
    }
}

fun BehandlingHendelse.SøknadMottatt.tilDomene(): BehandlingHendelseIntern {
    return BehandlingHendelseIntern.SøknadMottatt(
        sistEndret = this.sistEndret,
        progresjon = this.progresjon.tilDomene(),
    )
}
fun BehandlingHendelse.AlderLovvalgMedlemskap.tilDomene(): BehandlingHendelseIntern {
    return BehandlingHendelseIntern.AlderLovvalgMedlemskap(
        sistEndret = this.sistEndret,
        progresjon = this.progresjon.tilDomene(),
        status = when (this.status) {
            BehandlingHendelse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
            BehandlingHendelse.AlderLovvalgMedlemskap.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
            null -> null
        }
    )
}
fun BehandlingHendelse.NavKontor.tilDomene(): BehandlingHendelseIntern {
    return BehandlingHendelseIntern.NavKontor(
        sistEndret = this.sistEndret,
        progresjon = this.progresjon.tilDomene(),
        status = when (this.status) {
            BehandlingHendelse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING -> BehandlingHendelseIntern.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING
            BehandlingHendelse.NavKontor.Status.MOTTATT_LEGEERKLÆRING -> BehandlingHendelseIntern.NavKontor.Status.MOTTATT_LEGEERKLÆRING
            BehandlingHendelse.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING -> BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING
            null -> null
        }
    )
}
fun BehandlingHendelse.Nay.tilDomene(): BehandlingHendelseIntern {
   return BehandlingHendelseIntern.Nay(
       sistEndret = this.sistEndret,
       progresjon = this.progresjon.tilDomene(),
       status = when (this.status){
           BehandlingHendelse.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseIntern.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
           BehandlingHendelse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
           null -> null
       }
   )
}
fun BehandlingHendelse.FerdigBehandlet.tilDomene(): BehandlingHendelseIntern {
    return BehandlingHendelseIntern.FerdigBehandlet(
        sistEndret = this.sistEndret,
        progresjon = this.progresjon.tilDomene(),
    )
}
fun BehandlingHendelse.tilDomene(): BehandlingHendelseIntern = when (this) {
    is BehandlingHendelse.SøknadMottatt -> this.tilDomene()
    is BehandlingHendelse.AlderLovvalgMedlemskap -> this.tilDomene()
    is BehandlingHendelse.NavKontor -> this.tilDomene()
    is BehandlingHendelse.Nay -> this.tilDomene()
    is BehandlingHendelse.FerdigBehandlet -> this.tilDomene()
}
fun BehandlingVisningDto.tilDomene(): BehandlingVisningIntern {
    return BehandlingVisningIntern(
        behandlingReferanse = this.behandlingReferanse.toString(),
        avsluttet = this.avsluttet,
        behandlingType = when (this.behandlingType){
            BehandlingHendelseType.SØKNAD -> BehandlingVisningTypeIntern.SØKNAD
            BehandlingHendelseType.KLAGE -> BehandlingVisningTypeIntern.KLAGE
        },
        hendelser = this.hendelser.map { it.tilDomene() },
    )
}
