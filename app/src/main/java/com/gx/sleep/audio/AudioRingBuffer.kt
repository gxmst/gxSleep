package com.gx.sleep.audio

/**
 * Circular buffer for audio frames.
 * Avoids frequent allocations by reusing buffer slots.
 */
class AudioRingBuffer(private val capacity: Int) {

    private val buffer = ArrayDeque<AudioFrame>(capacity + 1)

    fun add(frame: AudioFrame) {
        if (buffer.size >= capacity) {
            buffer.removeFirst()
        }
        buffer.addLast(frame)
    }

    fun getAll(): List<AudioFrame> = buffer.toList()

    fun getLast(n: Int): List<AudioFrame> {
        val start = (buffer.size - n).coerceAtLeast(0)
        return buffer.toList().subList(start, buffer.size)
    }

    fun clear() {
        buffer.clear()
    }

    val size: Int get() = buffer.size

    fun isEmpty(): Boolean = buffer.isEmpty()
}
