package il.ac.technion.cs.softwaredesign.tests.messages

import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import java.nio.charset.Charset
import java.time.LocalDateTime

class MessageImpl(override val id: Long, override val media: MediaType, override val contents: ByteArray, override val created: LocalDateTime, override var received: LocalDateTime?) : Message {

    override fun toString(): String {
        val contentsStr = contents.toString(Charset.defaultCharset())
        return "$id%$media%$contentsStr%$created%$received"
    }
}