package api.vedtak.dagens

import java.time.LocalDate

// Her er hvordan dagens api ser ut, fra: https://confluence.adeo.no/display/ARENA/Arena+-+Tjeneste+Webservice+-+Ytelseskontrakt_v3


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