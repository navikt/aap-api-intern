package api.vedtak.dagens

import java.time.LocalDate

// Her er hvordan dagens api ser ut, fra: https://confluence.adeo.no/display/ARENA/Arena+-+Tjeneste+Webservice+-+Ytelseskontrakt_v3

data class DagensRequest(
    val ident: String,
    val periode: Periode,
    val tema: String = "AAP"
)

data class DagensResponse(
    val meldekortUtbetalingsgrunnlagListe: List<Sak> // Brukers saker
)

data class Sak(
    val vedtakListe: List<Vedtak>, // Vedtakene sammenfaller helt eller delvis med perioden det spørres på, dersom det er angitt en request-periode.
    val fagsystemSakId: String,
    val saksstatus: String, // Arena-interne koder (AKTIV, INAKT, AVSLU, HIST - ikke i bruk)
    val tema: String = "AAP"
)

data class Vedtak(
    val meldekortListe: List<Meldekort>?, // Meldekort som gjelder i vedtaksperioden. Listen vil være begrenset av periode angitt i request. Listen vil bare inneholde meldekort som har ført til utbetaling.
    val vedtaksperiode: Periode, // Periode vedtaket er gyldig, TOM kan være null
    val vedtaksstatus: String, // Kodeverdier fra Arena
    val vedtaksdato: LocalDate?, // Dato for når vedtaket er fattet, dersom vedtaket har vært iverksatt.
    val datoKravMottatt: LocalDate, // når krav/søknad ble registrert mottatt.
    val dagsats: Double // dagsats inkludert eventuelle barnetillegg. Beløpet skal vise full dagsats før eventuell samordning og uten reduksjon ifm. institusjonsopphold
)

data class Meldekort(
    val meldekortperiode: Periode,
    val dagsats: Double,
    val beløp: Double,
    val utbetalingsgrad: Double // Full utbetaling av periode er 200%
)

data class Periode(
    val fom: LocalDate?,
    val tom: LocalDate?
)

/*

Vedtaksstatuser:
OPPRE	Opprettet	Første status for et vedtak
MOTAT	Mottatt		Status når opplysninger i saken registreres
REGIS	Registrert	Status når prosess forbered vedtak utføres (vilkårsvurdering, samordning, fastsettelse av grunnlag og sats)
INNST	Innstilt	Vedtaket er sendt til beslutter
GODKJ	Godkjent	Ikke så mye benyttet, men kan settes av beslutter dersom beslutter har fått flere vedtak tilbeslutning i samme sak, og returnerer kun ett av dem til saksbehandler
IVERK	Iverksatt	Positivt vedtak er fattet, og løper
AVSLU	Avsluttet	Når vedtakets til-dato er passert, eller vedtaket er erstattet av et nytt vedtak

KONT	Etter kontroll	Benyttes av feilutbetalinger

 */