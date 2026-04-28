package no.nav.aap.api.kelvin

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import java.net.URI
import java.time.Duration
import no.nav.aap.api.Metrics
import no.nav.aap.api.Metrics.prometheus
import no.nav.aap.api.intern.NåværendeEnhet
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.retryablePost
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.oppgave.enhet.EnhetOgOversendelse
import no.nav.aap.oppgave.enhet.OppgaveKategori
import no.nav.aap.oppgave.enhet.PersonRequest

object OppgaveGateway {
    private val baseUrl = URI.create(requiredConfigForKey("INTEGRASJON_OPPGAVE_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_OPPGAVE_SCOPE"))

    private val client =
        RestClient.withDefaultResponseHandler(
            config = config,
            tokenProvider = AzureM2MTokenProvider,
        )

    private val cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .recordStats()
        .build<String, Pair<NåværendeEnhet?, String?>>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, cache, "oppgave_enhet_cache")
    }

    fun hentEnhetForPerson(
        personIdent: String,
    ): Pair<NåværendeEnhet?, String?> = cache.get(personIdent) {
        val httpRequest = PostRequest(PersonRequest(personIdent))

        val respons = requireNotNull(
            client.retryablePost<_, EnhetOgOversendelse>(
                uri = baseUrl.resolve("/enhet/status/person"),
                request = httpRequest,
            )
        )

        Metrics.enhetInformasjon(
            respons.tilstand != null,
            respons.tilstand?.oppgaveKategori?.name ?: "null"
        )

        respons.tilstand?.let {
            NåværendeEnhet(
                oversendtDato = it.oversendtDato,
                oppgaveKategori = when (it.oppgaveKategori) {
                    OppgaveKategori.MEDLEMSKAP -> no.nav.aap.api.intern.OppgaveKategori.MEDLEMSKAP
                    OppgaveKategori.LOKALKONTOR -> no.nav.aap.api.intern.OppgaveKategori.LOKALKONTOR
                    OppgaveKategori.KVALITETSSIKRING -> no.nav.aap.api.intern.OppgaveKategori.KVALITETSSIKRING
                    OppgaveKategori.NAY -> no.nav.aap.api.intern.OppgaveKategori.NAY
                    OppgaveKategori.BESLUTTER -> no.nav.aap.api.intern.OppgaveKategori.BESLUTTER
                },
                enhet = it.enhet,
                erHasteSak = it.markertSomHasteSak
            ) to it.saksnummer
        } ?: Pair(null, null)
    }
}
