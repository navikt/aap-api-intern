package no.nav.aap.api.util

import no.nav.aap.api.arena.IArenaoppslagGateway
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1
import no.nav.aap.arenaoppslag.kontrakt.intern.*
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.time.LocalDate

class FakeArenaGateway : IArenaoppslagGateway {
    override suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse {
        return PerioderResponse(emptyList())
    }

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest
    ): PerioderMed11_17Response {
        return PerioderMed11_17Response(emptyList())
    }

    override suspend fun hentPersonEksistererIAapContext(
        callId: String, req: SakerRequest
    ): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(false)
    }

    override suspend fun hentPersonHarSignifikantHistorikk(
        callId: String,
        req: SignifikanteSakerRequest
    ): SignifikanteSakerResponse {
        return SignifikanteSakerResponse(false, emptyList())
    }

    override suspend fun hentSakerByFnr(
        callId: String, req: SakerRequest
    ): List<SakStatus> {
        return emptyList()
    }

    override suspend fun hentSakerForPerson(
        callId: String, req: SakerRequestV1
    ): SakerResponse {
        return when (req.personidentifikator) {
            "01410028596" -> SakerResponse(
                listOf(
                    ArenaSakOppsummeringKontrakt(
                        sakId = "1",
                        lopenummer = 1,
                        aar = 2021,
                        antallVedtak = 1,
                        statuskode = "AKTIV",
                        statusnavn = "Aktiv",
                        sakstype = "Arbeidsavklaringspenger",
                        regDato = LocalDate.of(2022, 2, 2),
                        avsluttetDato = null,
                    )
                )
            )
            else -> SakerResponse(emptyList())
        }
    }

    override suspend fun hentMaksimum(
        callId: String, req: InternVedtakRequest
    ): Maksimum {
        return Maksimum(emptyList())
    }
}