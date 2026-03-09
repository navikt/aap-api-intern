package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.NåværendeEnhet
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.retryablePost
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.oppgave.enhet.EnhetOgOversendelse
import no.nav.aap.oppgave.enhet.OppgaveKategori
import no.nav.aap.oppgave.enhet.PersonRequest
import java.net.URI

object OppgaveGateway {
    private val baseUrl = URI.create(requiredConfigForKey("INTEGRASJON_OPPGAVE_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_OPPGAVE_SCOPE"))

    private val client =
        RestClient.withDefaultResponseHandler(
            config = config,
            tokenProvider = ClientCredentialsTokenProvider,
        )

    fun hentEnhetForPerson(
        personIdent: String,
    ): Pair<NåværendeEnhet, String>? {
        val personTilgangRequest =
            PersonRequest(personIdent)
        val httpRequest =
            PostRequest(
                body = personTilgangRequest
            )


        val respons = requireNotNull(
            client.retryablePost<_, EnhetOgOversendelse>(
                uri = baseUrl.resolve("/enhet/status/person"),
                request = httpRequest,
            )
        )

        val tilstand = respons.tilstand ?: return null

        return NåværendeEnhet(
            oversendtDato = tilstand.oversendtDato,
            oppgaveKategori = when (tilstand.oppgaveKategori) {
                OppgaveKategori.MEDLEMSKAP -> no.nav.aap.api.intern.OppgaveKategori.MEDLEMSKAP
                OppgaveKategori.LOKALKONTOR -> no.nav.aap.api.intern.OppgaveKategori.LOKALKONTOR
                OppgaveKategori.KVALITETSSIKRING -> no.nav.aap.api.intern.OppgaveKategori.KVALITETSSIKRING
                OppgaveKategori.NAY -> no.nav.aap.api.intern.OppgaveKategori.NAY
                OppgaveKategori.BESLUTTER -> no.nav.aap.api.intern.OppgaveKategori.BESLUTTER
            },
            enhet = tilstand.enhet,
        ) to tilstand.saksnummer
    }
}
