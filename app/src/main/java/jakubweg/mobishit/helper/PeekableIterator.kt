package jakubweg.mobishit.helper


class PeekableIterator <T> (private val iterator: Iterator<T>) : Iterator<T> {
    private var peeked: T? = null
    fun peek() = peeked ?: iterator.next().also { peeked = it }

    override fun hasNext() = peeked != null || iterator.hasNext()

    override fun next(): T {
        val temp = peeked ?: return iterator.next()
        peeked = null
        return temp
    }

}

fun <T> Iterator<T>.toPeekedIterator() = PeekableIterator(this)

fun <T> Iterable<T>.peekableIterator() = PeekableIterator(iterator())