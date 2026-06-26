package no.nav.aap.api.dsop

import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.api.intern.DsopMeldekortDTO
import no.nav.aap.api.intern.DsopRettighetsTypeDTO
import no.nav.aap.api.intern.DsopStatusDTO
import no.nav.aap.api.intern.DsopTimerArbeidetPerDagDTO
import no.nav.aap.api.intern.DsopVedtakDTO
import no.nav.aap.api.intern.DsopVedtaksTypeDTO
import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.intern.PeriodeNullableTomDTO
import no.nav.aap.api.intern.Utfall
import no.nav.aap.api.kelvin.Arenavedtak
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.Meldekort
import no.nav.aap.api.kelvin.MeldekortService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

class DsopService(
    private val behandlingsRepository: BehandlingsRepository,
    private val meldekortService: MeldekortService,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    constructor(
        connection: DBConnection,
        pdlGateway: IPdlGateway,
        clock: Clock = Clock.systemDefaultZone(),
    ) : this(
        behandlingsRepository = BehandlingsRepository(connection),
        meldekortService = MeldekortService(connection, pdlGateway, clock),
        clock = clock,
    )

    fun hentDsopVedtak(fnr: String, uttrekksperiode: Periode): List<DsopVedtakDTO> {
        val behandlinger = behandlingsRepository.hentVedtaksData(fnr, uttrekksperiode)

        return behandlinger.flatMap { behandling ->
            utledDsopVedtak(behandling, LocalDate.now(clock))
        }
    }

    fun hentDsopMeldekort(
        personIdent: String,
        periode: Periode,
    ): List<DsopMeldekortDTO> {
        return hentAlleMeldekortMedRett(personIdent, periode.fom, periode.tom)
            .map { meldekort ->
                DsopMeldekortDTO(
                    PeriodeDTO(meldekort.meldePeriode.fom, meldekort.meldePeriode.tom),
                    meldekort.arbeidPerDag.sumOf { it.timerArbeidet },
                    meldekort.arbeidPerDag.map {
                        DsopTimerArbeidetPerDagDTO(
                            it.dag,
                            it.timerArbeidet.toDouble()
                        )
                    },
                    meldekort.mottattTidspunkt
                )
            }.slåSammenMeldeperioder()
    }

    private fun hentAlleMeldekortMedRett(
        personIdentifikator: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<Meldekort> {
        val kelvinVedtak = hentDsopVedtak(
            personIdentifikator,
            Periode(fom, tom)
        )

        val meldekortListe = meldekortService.hentAlleMeldekort(personIdentifikator, fom, tom)

        val filtrerteMeldekort = meldekortListe.filter { meldekort ->
            kelvinVedtak.any {
                val periode = Periode(it.virkningsperiode.fom, it.virkningsperiode.tom ?: Tid.MAKS)
                periode.overlapper(
                    Periode(
                        meldekort.meldePeriode.fom,
                        meldekort.meldePeriode.tom
                    )
                )
            }
        }

        return filtrerteMeldekort
    }

    companion object {
        fun utledDsopVedtak(
            behandling: Behandling,
            now: LocalDate,
        ): List<DsopVedtakDTO> = outerJoinRunningFold<DsopRettighetsTypeDTO, Arenavedtak, DsopVedtakDTO>(
            behandling.rettighetsTypeTidslinje.map { DsopRettighetsTypeDTO.valueOf(it) },
            behandling.arenakompatibleVedtakTidslinje,
        ) { forrigeVedtak, periode, rettighetsType, arenavedtak ->
            if (arenavedtak != null) {
                require(
                    (rettighetsType == null) == (
                            arenavedtak.vedtaksvariant in setOf(
                                Arenavedtak.Vedtaksvariant.G_AVSLAG,
                                Arenavedtak.Vedtaksvariant.O_AVSLAG,
                                Arenavedtak.Vedtaksvariant.S_STANS,
                                Arenavedtak.Vedtaksvariant.S_OPPHOR,
                                Arenavedtak.Vedtaksvariant.S_DOD,
                            ))
                ) {
                    "sak ${behandling.sak.saksnummer}: Hvis vedtaksvariant er satt, så har man ikke rett til AAP (rettighetstype == null) hvis og bare hvis det er en variant som ikke gir rett til AAP"
                }
            }

            val vedtaksvariant = arenavedtak?.vedtaksvariant

            DsopVedtakDTO(
                vedtakId = arenavedtak?.vedtakId?.toString() ?: behandling.vedtakId.toString(),
                vedtakStatus = when (periode.tom >= now) {
                    true -> DsopStatusDTO.LØPENDE
                    else -> DsopStatusDTO.AVSLUTTET
                },
                virkningsperiode = when (vedtaksvariant) {
                    null,
                    Arenavedtak.Vedtaksvariant.O_INNV_NAV,
                    Arenavedtak.Vedtaksvariant.O_INNV_SOKNAD,
                    Arenavedtak.Vedtaksvariant.E_FORLENGE,
                    Arenavedtak.Vedtaksvariant.E_VERDI,
                    Arenavedtak.Vedtaksvariant.G_INNV_NAV,
                    Arenavedtak.Vedtaksvariant.G_INNV_SOKNAD ->
                        PeriodeNullableTomDTO(periode.fom, periode.tom)

                    Arenavedtak.Vedtaksvariant.O_AVSLAG,
                    Arenavedtak.Vedtaksvariant.G_AVSLAG,
                    Arenavedtak.Vedtaksvariant.S_DOD,
                    Arenavedtak.Vedtaksvariant.S_OPPHOR,
                    Arenavedtak.Vedtaksvariant.S_STANS -> {
                        require(periode.fom == periode.tom) {
                            """vedtakslengde ved avslag, stans og opphør har ingen sluttdato, som skal være 
                                            representert med fom == tom."""
                        }
                        PeriodeNullableTomDTO(periode.fom, null)
                    }
                },
                utfall = utfallFraVedtaksvariant(vedtaksvariant),
                aktivitetsfase = rettighetsType ?: requireNotNull(forrigeVedtak?.aktivitetsfase) {
                    """perioden $periode har ingen rettighetstype, som betyr at dette er et stansvedtak,  forrige vedtak
                        skal da være et vedtak som gir rett, og skal derfor ha aktivetetsfase definert."""
                },
                vedtaksType = vedtaksvariant?.type
                    ?: if (behandling.nyttVedtak) DsopVedtaksTypeDTO.O else DsopVedtaksTypeDTO.E,
                vedtaksvariant = vedtaksvariant?.somDTO,
            )
        }
            .verdier()
            .toList()
    }
}

fun utfallFraVedtaksvariant(vedtaksvariant: Arenavedtak.Vedtaksvariant?) = when (vedtaksvariant) {
    Arenavedtak.Vedtaksvariant.O_INNV_NAV -> Utfall.JA
    Arenavedtak.Vedtaksvariant.O_INNV_SOKNAD -> Utfall.JA
    Arenavedtak.Vedtaksvariant.O_AVSLAG -> Utfall.NEI
    Arenavedtak.Vedtaksvariant.E_FORLENGE -> Utfall.JA
    Arenavedtak.Vedtaksvariant.E_VERDI -> Utfall.JA
    Arenavedtak.Vedtaksvariant.G_INNV_NAV -> Utfall.JA
    Arenavedtak.Vedtaksvariant.G_INNV_SOKNAD -> Utfall.JA
    Arenavedtak.Vedtaksvariant.G_AVSLAG -> Utfall.NEI
    Arenavedtak.Vedtaksvariant.S_DOD -> Utfall.JA
    Arenavedtak.Vedtaksvariant.S_OPPHOR -> Utfall.JA
    Arenavedtak.Vedtaksvariant.S_STANS -> Utfall.JA

    /* Hvis vi ikke har vedtaksvariant, så er det data som ikke er back-fillet. Da returnerer vi alltid JA. */
    null -> Utfall.JA
}

fun List<DsopMeldekortDTO>.slåSammenMeldeperioder(): List<DsopMeldekortDTO> {
    return this
        .groupBy { it.periode }
        .values.map { meldekort ->
            DsopMeldekortDTO(
                periode = meldekort.first().periode,
                antallTimerArbeidet = BigDecimal.ZERO,
                timerArbeidetPerDag = meldekort.sortedBy { it.sistOppdatert }
                    .flatMap { it.timerArbeidetPerDag }
                    .groupingBy { it.dag }.reduce { _, accumulator, element ->
                        accumulator.copy(timerArbeidet = element.timerArbeidet)
                    }.values.toList(),
                sistOppdatert = meldekort.maxByOrNull { it.sistOppdatert }?.sistOppdatert
                    ?: LocalDateTime.MIN,
            ).let { meldekort ->
                meldekort.copy(antallTimerArbeidet = meldekort.timerArbeidetPerDag.sumOf { it.timerArbeidet }
                    .toBigDecimal())
            }
        }
}

fun <T1, T2, R> outerJoinRunningFold(
    t1: Tidslinje<T1>,
    t2: Tidslinje<T2>,
    f: (R?, Periode, T1?, T2?) -> R,
): Tidslinje<R> {
    var previous: R? = null
    return t1.outerJoin(t2) { p, x1, x2 ->
        f(previous, p, x1, x2).also {
            previous = it
        }
    }
}
