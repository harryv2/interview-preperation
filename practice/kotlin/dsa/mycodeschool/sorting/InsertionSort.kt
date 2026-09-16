package dsa.mycodeschool.sorting

/**
 * Insertion sort: grow a sorted prefix one element at a time.
 * Pick arr[i], shift larger elements of the sorted prefix right, drop it into the hole.
 *
 * Time: O(n^2) worst/avg, O(n) best (already sorted)
 * Space: O(1), stable
 */
fun insertionSort(arr: IntArray) {
    for (i in 1 until arr.size) {
        val value = arr[i]
        var hole = i
        while (hole > 0 && arr[hole - 1] > value) {
            arr[hole] = arr[hole - 1]
            hole--
        }
        arr[hole] = value
    }
}

fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)
    insertionSort(arr)
    println(arr.joinToString())
}
