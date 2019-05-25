package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class FakeSecureStorageModule  : KotlinModule() {
    override fun configure() {

        bind<SecureStorageFactory>().to<FakeSecureStorageFactory>().`in`<Singleton>()

    }
}

