package il.ac.technion.cs.softwaredesign.messages

import java.time.LocalDateTime

interface Message {
    val id: Long
    val media: MediaType
    val contents: ByteArray
    val created: LocalDateTime
    var received: LocalDateTime?
}