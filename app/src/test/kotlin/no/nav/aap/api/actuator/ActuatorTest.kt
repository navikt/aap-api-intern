package no.nav.aap.api.actuator

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.time.Duration
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.Motor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActuatorTest {

    @Test
    fun `live returnerer alltid 200`() = testApplication {
        application {
            routing { actuator(prometheus(), FakeMotor(kjørerSvar = false), IkkeTilkobletDataSource()) }
        }

        val response = client.get("/actuator/live")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `ready returnerer 200 når motor kjører og database er oppe`() = testApplication {
        val dataSource = TestDataSource()
        application {
            routing { actuator(prometheus(), FakeMotor(kjørerSvar = true), dataSource) }
        }

        val response = client.get("/actuator/ready")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        dataSource.close()
    }

    @Test
    fun `ready returnerer 503 med melding når motor ikke kjører`() = testApplication {
        val dataSource = TestDataSource()
        application {
            routing { actuator(prometheus(), FakeMotor(kjørerSvar = false), dataSource) }
        }

        val response = client.get("/actuator/ready")

        assertThat(response.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
        assertThat(response.bodyAsText()).contains("Motor kjører ikke")
        dataSource.close()
    }

    @Test
    fun `ready returnerer 503 med melding når database er nede`() = testApplication {
        application {
            routing { actuator(prometheus(), FakeMotor(kjørerSvar = true), IkkeTilkobletDataSource()) }
        }

        val response = client.get("/actuator/ready")

        assertThat(response.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
        assertThat(response.bodyAsText()).contains("Database ikke tilgjengelig")
    }

    @Test
    fun `metrics returnerer 200`() = testApplication {
        application {
            routing { actuator(prometheus(), FakeMotor(kjørerSvar = true), IkkeTilkobletDataSource()) }
        }

        val response = client.get("/actuator/metrics")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    private fun prometheus() = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

private class FakeMotor(private val kjørerSvar: Boolean) : Motor {
    override fun start() = Unit
    override fun stop(timeout: Duration) = Unit
    override fun kjører() = kjørerSvar
    override fun close() = Unit
}

private class IkkeTilkobletDataSource : DataSource {
    override fun getConnection(): Connection = throw SQLException("Ingen databasetilkobling")
    override fun getConnection(username: String?, password: String?): Connection = throw SQLException("Ingen databasetilkobling")
    override fun getLogWriter(): PrintWriter? = null
    override fun setLogWriter(out: PrintWriter?) = Unit
    override fun setLoginTimeout(seconds: Int) = Unit
    override fun getLoginTimeout(): Int = 0
    override fun getParentLogger(): Logger = Logger.getLogger("IkkeTilkobletDataSource")
    override fun <T : Any?> unwrap(iface: Class<T>?): T = throw UnsupportedOperationException()
    override fun isWrapperFor(iface: Class<*>?): Boolean = false
}
