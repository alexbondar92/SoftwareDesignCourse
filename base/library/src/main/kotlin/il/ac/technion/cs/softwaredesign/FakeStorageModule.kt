package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

/**
 * use this module in tests
 */
class FakeStorageModule : KotlinModule() {
    override fun configure() {
        bind<SecureStorageFactory>().to<FakeStorageFactory>()
    }
}