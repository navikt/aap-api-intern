package api

import api.util.Fakes
import api.util.port
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import oppslag.auth.TokenXProviderConfig
import java.net.URI

object TestConfig {
    internal val postgres = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
    )

    val azure = AzureConfig(
        tokenEndpoint = "http://localhost:${Fakes().azure.port()}/jwt",
        clientId = "test",
        clientSecret = "test",
        jwksUri = "test",
        issuer = "test",
    )

    fun default(fakes: Fakes): Config {
        return Config(
            arenaoppslag = ArenaoppslagConfig(
                proxyBaseUrl = "",
                scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default"
            ),
            kelvinConfig = KelvinConfig(
                proxyBaseUrl = "",
                scope = "api://behandlingsflyt/.default"
            ),
            azure = azure,
            dbConfig = postgres,
            tokenx = TokenXProviderConfig(
                clientId = "aap-oppslag",
                issuer = "tokenx",
                jwksUrl = URI.create("http://localhost:${fakes.tokenx.port()}/jwks").toURL(),
                privateKey = """{
            "kty": "RSA",
            "d": "MRf73iiXUEhJFxDTtJ5rEHNQsAG8XFuXkz9vXXbMp1_OTo11bEx3SnHiwmO_mSAAeXWNJniLw07V1-nk551h5in_ueAPwXTOf8qddacvDEBZwcxeqfu_Kjh1R0ji8Xn1a037CpH2IO34Lyw2gmsGFdMZgDwa5Z0KJjPCU6W8tF6CA-2omAdNzrFaWtaPFpBC0NzYaaB111bKIXxngG97Cnu81deEEKmX-vL-O4tpvUUybuquxrlFvVlTeYlrQqv50_IKsKSYkg-iu1cbqIiWrRq9eTmA6EppmZbqHjKSM5JYFbPB_oZ9QeHKnp1_MTom-jKMEpw18qq-PzdX_skZWQ",
            "e": "AQAB",
            "use": "sig",
            "kid": "localhost-signer",
            "alg": "RS256",
            "n": "lFTMP9TSUwLua0G8M7foqmdUS2us1-JOF8H_tClVG3IEQMRvMmHJoGSdldWDHsNwRG3Wevl_8fZoGocw9hPqj93j-vI4-ZkbxwhPyRqlS0FNIPD1Ln5R6AmHu7b-paRIz3lvqpyTRwnGBI9weE4u6WOpOQ8DjJMNPq4WcM42AgDJAvc6UuhcWW_MLIsjkKp_VYKxzthSuiRAxXi8Pz4ZhiTAEZI-UN61DYU9YEFNujg5XtIQsRwQn1Vj7BknGwkdf_iCGJgDlKUOz9hAojOMXTAwetUx6I5nngIM5vaXWJCmKn6SzcTYgHWWVrn8qaSazioaydLaYN9NuQ0MdIvsQw"
        }""",
                tokenEndpoint = "http://localhost:${fakes.tokenx.port()}/token",
            ),
        )
    }

}