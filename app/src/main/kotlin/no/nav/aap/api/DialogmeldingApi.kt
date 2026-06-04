package no.nav.aap.api

import behandlingsflyt.DialogmeldingEksistererDto
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import java.util.UUID
import no.nav.aap.api.kelvin.DokumentinnhentingGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedGet

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

fun NormalOpenAPIRoute.dialogmeldingApi(dokumentinnhentingGateway: DokumentinnhentingGateway) {
    route("/dialogmelding") {
        route("/{dialogmeldingId}/eksisterer").authorizedGet<DialogmeldingIdParameter, DialogmeldingEksistererDto>(
            AuthorizationMachineToMachineConfig(
                listOf(UUID.fromString(requiredConfigForKey("AZP_PADM2")))
            )
        ) { params ->
            val dto = dokumentinnhentingGateway.dialogmeldingEksisterer(params.dialogmeldingId)

            respond(dto)
        }

    }
}
