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

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#ytelse-aap-værsågod](https://nav-it.slack.com/archives/C0312J501GX/p1740730504702299).
