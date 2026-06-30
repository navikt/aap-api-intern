package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.*
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import java.time.Clock
import java.time.LocalDate

class NksMeldeperioderService(
    connection: DBConnection,
    private val pdlGateway: IPdlGateway,
    clock: Clock,
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
        if (personIdenter.isEmpty()) return NksMeldeperioderResponse(emptyList())

        val søkeperiode = Periode(
            fom ?: LocalDate.of(1900, 1, 1),
            tom ?: LocalDate.of(9999, 12, 31),
        )
        val behandling =
            personIdenter.flatMap { behandlingsRepository.hentVedtaksData(it, søkeperiode) }
                .maxByOrNull { it.vedtakId }

        val meldekort = meldekortService.hentAlleMeldekortMedMeldeperiodeEllerMottattIPeriode(
            personIdentifikator, fom, tom
        )

        val underveistidslinje = behandling?.underveisTidslinje.orEmpty().komprimer()

        val fritakMeldepliktTidslinje = behandling?.fritakMeldepliktTidslinje.orEmpty().komprimer()

        val tilkjentYtelseTidslinje = behandling?.tilkjent.orEmpty()

        val meldeperioder = underveistidslinje.map { it.meldeperiode }.perioder()

        val meldeperioderÅReturnere =
            underveistidslinje.splittOppIPerioder(meldeperioder.toList())
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
                .first()
                .let {
                    NksArbeidsgrad(
                        grad = it.verdi.first,
                        overGrenseVerdi = it.verdi.second,
                    )
                },
            dagsatser = tilkjentYtelseTidslinje.begrensetTil(meldeperiode).komprimer().segmenter()
                .map {
                    NksDagsats(
                        dato = it.periode.fom,
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
        )
    }

    private fun Meldekort.harArbeidsregistreringIPeriode(periode: Periode): Boolean {
        return arbeidPerDag.any { it.dag in periode.fom..periode.tom }
    }
}
