package lld.rippling.mediansorted

fun main() {
    val eng = intArrayOf(90, 120, 150, 200)
    val sales = intArrayOf(60, 80, 130)
    val hr = intArrayOf(70, 75, 110, 300)

    println("-- two departments")
    println("  median=${SortedMedian.ofTwo(eng, sales)}")
    println("  median=${SortedMedian.ofTwo(intArrayOf(1, 2), intArrayOf(3, 4))}")
    println("  median=${SortedMedian.ofTwo(intArrayOf(), intArrayOf(5))}")

    println("-- n departments, binary search on value")
    println("  median=${SortedMedian.ofMany(listOf(eng, sales, hr))}")

    println("-- n streams, k way merge stops at the middle")
    val streams = listOf(eng, sales, hr).map { it.iterator() }
    println("  median=${SortedMedian.ofStreams(streams, eng.size + sales.size + hr.size)}")

    println("-- unknown size, values arrive one by one")
    val running = RunningMedian()
    for (salary in listOf(100, 50, 300, 80, 120)) {
        running.add(salary)
        println("  after $salary median=${running.median()}")
    }
}
