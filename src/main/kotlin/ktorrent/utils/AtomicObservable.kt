package ktorrent.utils

class AtomicObservable<T>(initialValue: T) {

    val observers: MutableSet<(oldValue: T, newValue: T) -> Unit> = mutableSetOf()

    @Volatile var value = initialValue

        private set

    @Synchronized fun update(updater: (T) -> T) {
        val oldValue = value
        value = updater(value)
        if (oldValue != value) {
            observers.forEach { it(oldValue, value) }
        }
    }
}
