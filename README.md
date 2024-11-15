# AAP-API-intern
AAP-API-intern tilbyr AAP informasjon til interne konsumenter.

API’et vil hente data fra 2 kilder

Arena – det eksisterende saksbehandlingsverktøyet NAV bruker til å behandle AAP-saker
Kelvin – nytt saksbehandlingsverktøy som nå er under utvikling
Når Kelvin er ferdig vil data begynne å flyte derfra, men tanken er at dette ikke skal merkes av konsumentene.

**Skal kun kalles fra våre egne interne apper, ikke lag åpning for apper utenfor vårt namespace.**
Intern dokumentasjon kan finnes på [aap-sysdoc](https://aap-sysdoc.ansatt.nav.no/funksjonalitet/Datadeling/funksjonell).

# Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`
Det er ikke lagt opp til at denne skal kjøre standalone, siden den er avhengig av Arena.
All logikk ligger i testene.

API-specs:
- [Dev](https://aap-api.intern.dev.nav.no/swagger-ui/index.html)
- [Prod](https://aap-api.intern.nav.no/swagger-ui/index.html)

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #po-aap-team-aap.
