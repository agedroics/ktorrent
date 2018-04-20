package ktorrent.tracker

enum class EventType(val value: String) {

    STARTED("started"),
    STOPPED("stopped"),
    COMPLETED("completed"),
    NOT_SPECIFIED("")
}
