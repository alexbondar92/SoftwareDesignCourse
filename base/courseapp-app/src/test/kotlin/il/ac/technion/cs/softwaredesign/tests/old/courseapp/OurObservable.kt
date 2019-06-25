package il.ac.technion.cs.softwaredesign.tests.old.courseapp

abstract class OurObservable<T,V,R>{
    private val listeners = HashSet<(T,V) -> R>()
    fun listen(listener: (T,V) -> R){
        listeners.add(listener)
    }
    fun unlisten(listener: (T,V) -> R){
        listeners.remove(listener)
    }
    fun onChange(t: T, v: V) {
        listeners.forEach { it(t, v)}
    }

    fun isEmpty(): Boolean {
        return listeners.size == 0
    }

    fun contains(listener: (T,V) -> R): Boolean {
        return listeners.contains(listener)
    }

    fun toList (): List<(T,V) -> R> = listeners.toList()
}