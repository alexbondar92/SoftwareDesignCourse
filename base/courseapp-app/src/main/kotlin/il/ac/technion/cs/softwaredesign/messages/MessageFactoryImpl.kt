package il.ac.technion.cs.softwaredesign.messages

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.CourseAppImpl
import il.ac.technion.cs.softwaredesign.DataStoreIo
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

        val message = MessageImpl(id = newIndex, media = media, contents = contents, created = LocalDateTime.now(), received = null)

        storageIo.write(("MsgIndex$newIndex"), message.toString()).get()    // index to message & opposite(?)
        storageIo.write(("IndexMessageSys"), newIndex.toString()).get()    // newIndex

        return CompletableFuture.completedFuture(message)
    }
}