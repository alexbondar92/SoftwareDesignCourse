package il.ac.technion.cs.softwaredesign.messages

import java.util.concurrent.CompletableFuture

interface MessageFactory {
    /**
     * Create a [Message] with a unique ID using the provided [media] and [contents], which is marked as being sent now,
     * and not being received (null).
     */
    fun create(media: MediaType, contents: ByteArray): CompletableFuture<Message>
}