package com.yg.timeanchor.ring

/** 多个闹钟同一时刻到点：排队，不互相打断（需求 6）。 */
data class RingItem(val id: Int, val label: String, val desc: String, val triggerAt: Long)

object RingQueue {
    private val items = ArrayList<RingItem>()

    @Synchronized fun push(i: RingItem) { items.add(i) }

    @Synchronized fun peek(): RingItem? = items.firstOrNull()

    @Synchronized fun remove(id: Int) { items.removeAll { it.id == id } }

    @Synchronized fun size(): Int = items.size

    @Synchronized fun all(): List<RingItem> = items.toList()

    @Synchronized fun removeAllOf(list: Collection<RingItem>) {
        list.forEach { r -> items.removeAll { it.id == r.id } }
    }
}
