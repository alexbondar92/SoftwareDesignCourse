package il.ac.technion.cs.softwaredesign.tests.messages

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.old.courseapp.DataStoreIo
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class MessageFactoryImpl: MessageFactory {

    private val storageIo: DataStoreIo

    @Inject
    constructor(storage: DataStoreIo) {
        storageIo = storage
    }

    /**
     * Create a [Message] with a unique ID using the provided [media] and [contents], which is marked as being sent now,
     * and not being received (null).
     */
    override fun create(media: MediaType, contents: ByteArray): CompletableFuture<Message> {
        val str = storageIo.read(("IndexMessageSys")).get()

        val previousIndex = str?.toLong() ?: 0
        val newIndex = previousIndex + 1
        val time = LocalDateTime.now(Clock.systemDefaultZone())

        val message = MessageImpl(id = newIndex, media = media, contents = contents, created = time, received = null)

        storageIo.write(("MsgIndex$newIndex"), message.toString()).get()    // index to message & opposite(?)
        storageIo.write(("IndexMessageSys"), newIndex.toString()).get()    // newIndex

        return CompletableFuture.completedFuture(message)
    }
}