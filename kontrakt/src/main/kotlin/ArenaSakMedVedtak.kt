package no.nav.aap.api.intern

import java.time.LocalDate
import java.time.LocalDateTime

public data class ArenaSakMedVedtakResponse(
    val sakId: String,
    val opprettetAar: Int,
    val lopenr: Int,
    val person: ArenaSakPerson,
    val statuskode: String,
    val statusnavn: String,
    val registrertDato: LocalDateTime,
    val avsluttetDato: LocalDateTime?,
    val vedtak: List<ArenaVedtakDetaljer>,
)

public data class ArenaSakPerson(
    val personId: Int,
    val fodselsnummer: String,
    val fornavn: String,
    val etternavn: String,
)

public data class ArenaVedtakDetaljer(
    val vedtakId: Int,
    val lopenrvedtak: Int,
    val statusKode: String,
    val statusNavn: String,
    val vedtaktypeKode: String,
    val vedtaktypeNavn: String,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String,
    val fraOgMed: LocalDate?,
    val tilDato: LocalDate?,
    val rettighetkode: String,
    val rettighetnavn: String,
    val utfallkode: String?,
    val begrunnelse: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val relatertVedtak: Int?,
    val fakta: List<ArenaVedtakfakta>,
)

public data class ArenaVedtakfakta(
    val kode: String,
    val navn: String,
    val verdi: String?,
    val registrertDato: LocalDate,
)
