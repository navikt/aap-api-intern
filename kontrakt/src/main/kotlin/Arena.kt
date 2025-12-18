package no.nav.aap.api.intern

public data class ArenaStatusResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)