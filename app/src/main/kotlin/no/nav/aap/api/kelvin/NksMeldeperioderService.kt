package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.*
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.somTidslinje
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
        val behandlinger =
            personIdenter.flatMap { behandlingsRepository.hentVedtaksData(it, søkeperiode) }
                .distinctBy { it.vedtakId }

        val meldekort = meldekortService.hentAlleMeldekortMedMeldeperiodeEllerMottattIPeriode(
            personIdentifikator, fom, tom
        )

        val meldeperioder = behandlinger.flatMap { behandling ->
                behandling.underveisTidslinje.begrensetTil(søkeperiode).map { underveisperiode ->
                        underveisperiode.tilNksMeldeperiode(
                            behandling = behandling,
                            meldekort = meldekort,
                        )
                    }.segmenter().map { it.verdi }
            }.sortedWith(compareBy<NksMeldeperiode> { it.fraDato }.thenBy { it.tilDato })

        return NksMeldeperioderResponse(meldeperioder)
    }

    private fun Underveisperiode.tilNksMeldeperiode(
        behandling: Behandling,
        meldekort: List<Meldekort>,
    ): NksMeldeperiode {
        return NksMeldeperiode(
            fraDato = periode.fom,
            tilDato = periode.tom,
            fritakMeldeplikt = behandling.perioderMedFritakMeldeplikt.somTidslinje { it }
                .begrensetTil(periode).komprimer().segmenter()
                .map { NksDatoperiode(it.periode.fom, it.periode.tom) },
            meldekortMedTimer = meldekort.filter { it.harArbeidsregistreringIPeriode(periode) }
                .map { meldekort ->
                    NksMeldekortMedTimer(
                        journalPostId = meldekort.journalpostId,
                        mottattDato = meldekort.mottattTidspunkt.toLocalDate(),
                    )

                },
            meldekortLevertIMeldeperioden = meldekort.filter { it.mottattTidspunkt.toLocalDate() in periode.fom..periode.tom }
                .map { meldekort ->
                    NksMeldekortMedTimer(
                        journalPostId = meldekort.journalpostId,
                        mottattDato = meldekort.mottattTidspunkt.toLocalDate(),
                    )
                },
            timerArbeid = listOf(
                NksTimerArbeid(
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    timerArbeidet = timerArbeidet,
                )
            ),
            arbeidsgrad = NksArbeidsgrad(
                grad = arbeidsgrad,
                overGrenseVerdi = overgrenseVerdi,
            ),
            dagsatser = behandling.tilkjent.begrensetTil(periode).segmenter().map {
                    NksDagsats(
                        dato = it.periode.fom,
                        dagsats = it.verdi.dagsats,
                        effektivDagsats = it.verdi.effektivDagsats
                            ?: (it.verdi.dagsats * it.verdi.gradering / 100),
                        gradering = it.verdi.gradering,
                    )
                },
            meldeplikt = meldepliktstatus?.let {
                listOf(
                    Meldeplikt(
                        fraDato = periode.fom,
                        tilDato = periode.tom,
                        status = it,
                    )
                )
            }.orEmpty(),
        )
    }

    private fun Meldekort.harArbeidsregistreringIPeriode(periode: Periode): Boolean {
        return arbeidPerDag.any { it.dag in periode.fom..periode.tom }
    }
}
