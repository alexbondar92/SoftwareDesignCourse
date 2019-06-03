package il.ac.technion.cs.softwaredesign.messages

/**
 * Type of media included in a message.
 */
enum class MediaType {
    TEXT,
    FILE,
    PICTURE,
    STICKER,
    AUDIO,
    LOCATION,
    REFERENCE;

    // You're allowed to add methods and properties, but not to add or remove enum values.
}