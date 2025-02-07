package api.kelvin

import api.ArenaoppslagConfig
import api.KelvinConfig
import api.maksimum.Vedtak
import api.perioder.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.ktor.client.auth.azure.AzureConfig

class KelvinClient(
    private val kelvinConfig: KelvinConfig,
    azureConfig: AzureConfig
) {
    fun hentSakerByFnr(req: SakerRequest): List<SakStatus> {
        return emptyList()
    }

    fun hentMaksimum(req: InternVedtakRequest): List<Vedtak> {
        return emptyList()
    }
}