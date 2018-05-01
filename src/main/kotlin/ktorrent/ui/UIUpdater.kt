package ktorrent.ui

object UIUpdater : Runnable {

    val observables = mutableSetOf<UIObservable<*>>()

    override fun run() {
        observables.forEach {
            if (it.changesPending) {
                it.fireChanges()
            }
        }
    }
}
