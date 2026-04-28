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
import no.nav.aap.api.kelvin.Arenavedtak
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.Meldekort
import no.nav.aap.api.kelvin.MeldekortService
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
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

        return behandlinger.flatMap {
            it.rettighetsTypeTidslinje
                .segmenter()
                .map { (periode, verdi) -> RettighetsTypePeriode(periode.fom, periode.tom, verdi) }
                .map { rettighetsTypePeriode ->
                    DsopVedtakDTO(
                        vedtakId = it.vedtakId.toString(),
                        vedtakStatus = when (rettighetsTypePeriode.tom >= LocalDate.now()) {
                            true -> DsopStatusDTO.LØPENDE
                            else -> DsopStatusDTO.AVSLUTTET
                        },
                        virkningsperiode = PeriodeNullableTomDTO(
                            rettighetsTypePeriode.fom,
                            rettighetsTypePeriode.tom
                        ),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.valueOf(rettighetsTypePeriode.verdi),
                        vedtaksType = if (it.nyttVedtak) DsopVedtaksTypeDTO.O else DsopVedtaksTypeDTO.E,
                        vedtaksvariant = null,
                    )
                }
        }
    }

    fun hentDsopVedtakNy(fnr: String, uttrekksperiode: Periode): List<DsopVedtakDTO> {
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
        ): List<DsopVedtakDTO> =
            behandling.rettighetsTypeTidslinje.outerJoin(behandling.arenakompatibleVedtakTidslinje) { periode, rettighetsType, arenavedtak ->
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
                        "Hvis vedtaksvariant er satt, så har man ikke rett til AAP (rettighetstype == null) hvis og bare hvis det er en variant som ikke gir rett til AAP"
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
                    utfall = if (rettighetsType == null) "NEI" else "JA",
                    aktivitetsfase = rettighetsType?.let { DsopRettighetsTypeDTO.valueOf(it) },
                    vedtaksType = vedtaksvariant?.type
                        ?: if (behandling.nyttVedtak) DsopVedtaksTypeDTO.O else DsopVedtaksTypeDTO.E,
                    vedtaksvariant = vedtaksvariant?.somDTO,
                )
            }
                .verdier()
                .toList()
    }
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
