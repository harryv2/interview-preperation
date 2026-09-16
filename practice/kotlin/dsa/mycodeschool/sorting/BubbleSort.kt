package dsa.mycodeschool.sorting

/**
 * Bubble sort: repeatedly swap adjacent out-of-order elements.
 * After pass k, the largest k elements are in their final place at the end.
 *
 * Time: O(n^2) worst/avg, O(n) best (already sorted, thanks to the swapped flag)
 * Space: O(1), stable
 */
fun bubbleSort(arr: IntArray) {
    val n = arr.size
    for (k in 1 until n) {
        var swapped = false
        // last k-1 elements are already in place
        for (i in 0 until n - k) {
            if (arr[i] > arr[i + 1]) {
                val tmp = arr[i]
                arr[i] = arr[i + 1]
                arr[i + 1] = tmp
                swapped = true
            }
        }
        if (!swapped) break
    }
}

fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)
    bubbleSort(arr)
    println(arr.joinToString())
}
