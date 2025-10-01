# AAP API Intern
AAP-API-intern tilbyr AAP informasjon til interne konsumenter.

API’et henter data fra 2 kilder

Arena – det eksisterende saksbehandlingsverktøyet NAV bruker til å behandle AAP-saker
Kelvin – nytt saksbehandlingsverktøy

Intern dokumentasjon kan finnes på [aap-sysdoc](https://aap-sysdoc.ansatt.nav.no/funksjonalitet/Datadeling/funksjonell).

# Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`
Det er ikke lagt opp til at denne skal kjøre standalone, siden den er avhengig av Arena.
All logikk ligger i testene.

API-specs:
- [Dev](https://aap-api.intern.dev.nav.no/swagger-ui/index.html)
- [Prod](https://aap-api.intern.nav.no/swagger-ui/index.html)

# Metrics og Monitoring

## Circuit Breaker Metrics
Applikasjonen eksponerer Prometheus-metrics for circuit breakers som kan brukes i Grafana dashboards.
Følgende metrics er tilgjengelige:

- `resilience4j_circuitbreaker_state`: Viser tilstanden til circuit breaker (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_calls`: Antall kall kategorisert etter resultat (successful, failed, slow, timeout)
- `resilience4j_circuitbreaker_failure_rate`: Feilrate i prosent
- `resilience4j_circuitbreaker_slow_call_rate`: Rate av trege kall i prosent

For å skille mellom ulike circuit breakers kan man bruke labelen `name` (f.eks. "arenaoppslag-circuit-breaker").


### Eksempel på Grafana Query
For å vise om en circuit breaker er åpen eller halv-åpen:
```
resilience4j_circuitbreaker_state{name="arenaoppslag-circuit-breaker"} > 0
```

For å vise feilrate:
```
resilience4j_circuitbreaker_failure_rate{name="arenaoppslag-circuit-breaker"}
```

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#ytelse-aap-værsågod](https://nav-it.slack.com/archives/C0312J501GX/p1740730504702299).
