package no.nav.aap.api.util

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

class FakesExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        Fakes.start()
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(FakesExtension::class)
annotation class WithFakes
