package dsa.mycodeschool.sorting

import kotlin.random.Random

/**
 * Pick a random element of arr(start..end) as the pivot by swapping it into arr[end],
 * then reuse the plain Lomuto partition from QuickSort.kt.
 *
 * With a fixed pivot (always arr[end]) an already-sorted input makes every partition
 * peel off one element, giving O(n^2). A random pivot makes that input as unlikely as any
 * other, so the expected cost is O(n) regardless of input order.
 */
fun randomizedPartition(arr: IntArray, start: Int, end: Int): Int {
    val randomIndex = Random.nextInt(start, end + 1)
    swap(arr, randomIndex, end)
    return partition(arr, start, end)
}

/**
 * Quick select: same partition step as quick sort, but only recurse into the side
 * that contains targetIndex instead of both sides.
 *
 * After it returns, arr[targetIndex] holds the value that would be there if arr were sorted:
 * everything to its left is <= it, everything to its right is >= it (neither side is sorted).
 *
 * Time: O(n) expected for any input (random pivot), O(n^2) worst case but only with bad luck
 * Space: O(log n) expected recursion depth
 */
fun quickSelectActual(arr: IntArray, start: Int, end: Int, targetIndex: Int) {
    if (start >= end) {
        return
    }

    val pIndex = randomizedPartition(arr, start, end)

    when {
        pIndex == targetIndex -> return
        pIndex < targetIndex -> quickSelectActual(arr, pIndex + 1, end, targetIndex)
        else -> quickSelectActual(arr, start, pIndex - 1, targetIndex)
    }
}

fun quickSelect(arr: IntArray, targetIndex: Int) {
    require(targetIndex in arr.indices) { "targetIndex $targetIndex out of range for size ${arr.size}" }
    quickSelectActual(arr, 0, arr.lastIndex, targetIndex)
}

/**
 * The k largest elements of arr, in no particular order. Mutates arr.
 *
 * The k largest live in the last k slots of the sorted array, i.e. indices [n-k, n-1].
 * Partition until index n-k is in place, then everything from there to the end is >= it.
 */
fun topK(arr: IntArray, k: Int): IntArray {
    require(k in 0..arr.size) { "k=$k must be between 0 and ${arr.size}" }
    if (k == 0) return IntArray(0)

    val boundary = arr.size - k
    quickSelect(arr, boundary)
    return arr.copyOfRange(boundary, arr.size)
}

/** k-th largest element (k=1 is the max). Mutates arr. */
fun kthLargest(arr: IntArray, k: Int): Int {
    require(k in 1..arr.size) { "k=$k must be between 1 and ${arr.size}" }
    val targetIndex = arr.size - k
    quickSelect(arr, targetIndex)
    return arr[targetIndex]
}


fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)

    println("top 3: " + topK(arr.copyOf(), 3).joinToString())        // 4, 5, 8 in some order
    println("2nd largest: " + kthLargest(arr.copyOf(), 2))            // 5

    val sorted = arr.copyOf()
    quickSelect(sorted, 3)
    println("after quickSelect(3): " + sorted.joinToString())         // index 3 holds 2, left <= 2, right >= 2
}
