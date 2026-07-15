package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.Meldeplikt
import no.nav.aap.api.intern.NksArbeidsgrad
import no.nav.aap.api.intern.NksDagsats
import no.nav.aap.api.intern.NksDatoperiode
import no.nav.aap.api.intern.NksMeldekortMedTimer
import no.nav.aap.api.intern.NksMeldeperiode
import no.nav.aap.api.intern.NksMeldeperioderResponse
import no.nav.aap.api.intern.NksTimerArbeid
import no.nav.aap.api.intern.ÅrsakTilReduksjon
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Clock
import java.time.LocalDate

class NksMeldeperioderService(
    connection: DBConnection,
    private val pdlGateway: IPdlGateway,
    private val clock: Clock,
) {
    private val behandlingsRepository = BehandlingsRepository(connection)
    private val meldekortService = MeldekortService(connection, pdlGateway, clock)

    fun hent(
        personIdentifikator: String,
        fom: LocalDate?,
        tom: LocalDate?,
    ): NksMeldeperioderResponse {
        val personIdenter =
            pdlGateway.hentAlleIdenterForPerson(personIdentifikator).map { it.ident }
                .ifEmpty { return NksMeldeperioderResponse(emptyList()) }

        val søkeperiode = Periode(
            fom ?: Tid.MIN,
            minOf(tom ?: LocalDate.now(clock), LocalDate.now(clock))
        )
        val behandling =
            personIdenter.flatMap { behandlingsRepository.hentVedtaksData(it, søkeperiode) }
                .maxByOrNull { it.vedtakId }

        val meldekort = meldekortService.hentAlleMeldekortMedMeldeperiodeEllerMottattIPeriode(
            personIdentifikator, fom, tom
        )

        val underveistidslinje = behandling?.underveisTidslinje.orEmpty().komprimer()
            .begrensetTil(søkeperiode)

        val fritakMeldepliktTidslinje = behandling?.fritakMeldepliktTidslinje.orEmpty().komprimer()

        val tilkjentYtelseTidslinje = behandling?.tilkjent.orEmpty()

        val meldeperioder = underveistidslinje.map { it.meldeperiode }.perioder()

        val meldeperioderÅReturnere =
            underveistidslinje.splittOppIPerioder(
                meldeperioder.toList().filter { !it.inneholder(LocalDate.now(clock)) })
                .map { periode, underveisperiode ->
                    underveisperiode.tilNksMeldeperiode(
                        meldeperiode = periode,
                        fritakMeldepliktTidslinje = fritakMeldepliktTidslinje,
                        meldekort = meldekort,
                        tilkjentYtelseTidslinje = tilkjentYtelseTidslinje
                    )
                }
                .segmenter().map { it.verdi }

        return NksMeldeperioderResponse(meldeperioderÅReturnere)
    }

    private fun Tidslinje<Underveisperiode>.tilNksMeldeperiode(
        meldekort: List<Meldekort>,
        fritakMeldepliktTidslinje: Tidslinje<Boolean>,
        tilkjentYtelseTidslinje: Tidslinje<TilkjentYtelse>,
        meldeperiode: Periode,
    ): NksMeldeperiode {
        return NksMeldeperiode(
            fraDato = meldeperiode.fom,
            tilDato = meldeperiode.tom,
            fritakMeldeplikt = fritakMeldepliktTidslinje.begrensetTil(meldeperiode).komprimer()
                .segmenter().map { NksDatoperiode(it.periode.fom, it.periode.tom) },
            meldekortMedTimer = meldekort.filter { it.harArbeidsregistreringIPeriode(meldeperiode) }
                .map { meldekort ->
                    NksMeldekortMedTimer(
                        journalPostId = meldekort.journalpostId,
                        mottattDato = meldekort.mottattTidspunkt.toLocalDate(),
                    )

                },
            meldekortLevertIMeldeperioden = meldekort.filter {
                it.mottattTidspunkt.toLocalDate() in meldeperiode.fom..meldeperiode.tom
            }.map { meldekort ->
                NksMeldekortMedTimer(
                    journalPostId = meldekort.journalpostId,
                    mottattDato = meldekort.mottattTidspunkt.toLocalDate(),
                )
            },
            timerArbeid = map { it.timerArbeidet }.komprimer().segmenter().map {
                NksTimerArbeid(
                    periodeFom = it.periode.fom,
                    periodeTom = it.periode.tom,
                    timerArbeidet = it.verdi
                )
            },
            arbeidsgrad = map { Pair(it.arbeidsgrad, it.overgrenseVerdi) }.komprimer().segmenter()
                .also { require(it.toList().size == 1) { "Forventet én arbeidsgrad per meldeperiode, men fant ${it.toList().size}" } }
                .single()
                .let {
                    NksArbeidsgrad(
                        grad = it.verdi.first,
                        overGrenseverdi = it.verdi.second,
                    )
                },
            dagsatser = tilkjentYtelseTidslinje.begrensetTil(meldeperiode).komprimer().segmenter()
                .map {
                    NksDagsats(
                        periodeFom = it.periode.fom,
                        periodeTom = it.periode.tom,
                        dagsats = it.verdi.dagsats,
                        effektivDagsats = it.verdi.effektivDagsats
                            ?: (it.verdi.dagsats * it.verdi.gradering / 100),
                        gradering = it.verdi.gradering,
                    )
                },
            meldeplikt = mapNotNull { it.meldepliktstatus }.komprimer().segmenter().map {
                Meldeplikt(
                    fraDato = it.periode.fom,
                    tilDato = it.periode.tom,
                    status = it.verdi,
                )
            },
            aarsakerTilReduksjon = buildList {
                if (segmenter().any { it.verdi.meldepliktstatus == "IKKE_MELDT_SEG" }) {
                    add(ÅrsakTilReduksjon.BRUDD_PAA_MELDEPLIKT)
                }
                if (segmenter().any { it.verdi.overgrenseVerdi }) {
                    add(ÅrsakTilReduksjon.ARBEID_OVER_GRENSEVERDI)
                }
                if (segmenter().any { it.verdi.arbeidsgrad > 0 }) {
                    add(ÅrsakTilReduksjon.ARBEID)
                }
            },
        )
    }

    private fun Meldekort.harArbeidsregistreringIPeriode(periode: Periode): Boolean {
        return arbeidPerDag.any { it.dag in periode.fom..periode.tom }
    }
}