package ktorrent.ui

import javafx.beans.InvalidationListener
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.value.ChangeListener
import ktorrent.utils.AtomicObservable

class UIObservable<T>(private val observable: AtomicObservable<T>) : ReadOnlyProperty<T> {

    private val changeListeners = mutableSetOf<ChangeListener<in T>>()
    private val invalidationListeners = mutableSetOf<InvalidationListener>()

    var changesPending = false

    init {
        observable.listeners += { _, _ ->
            changesPending = true
        }
        UIUpdater.observables += this
    }

    fun fireChanges() {
        changesPending = false
        invalidationListeners.forEach {
            it.invalidated(this)
        }
    }

    override fun removeListener(listener: ChangeListener<in T>?) {
        listener?.let { changeListeners -= it }
    }

    override fun removeListener(listener: InvalidationListener?) {
        listener?.let { invalidationListeners -= it }
    }

    override fun getName() = ""

    override fun addListener(listener: ChangeListener<in T>?) {
        listener?.let { changeListeners += it }
    }

    override fun addListener(listener: InvalidationListener?) {
        listener?.let { invalidationListeners += it }
    }

    override fun getBean() = null

    override fun getValue() = observable.value
}
