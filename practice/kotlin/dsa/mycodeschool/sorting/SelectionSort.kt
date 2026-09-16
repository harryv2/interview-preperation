package dsa.mycodeschool.sorting

/**
 * Selection sort: for each position i, find the minimum of arr[i..n-1] and swap it into i.
 *
 * Time: O(n^2) in all cases
 * Space: O(1), not stable (swap can jump an equal element over another)
 */
fun selectionSort(arr: IntArray) {
    val n = arr.size
    for (i in 0 until n - 1) {
        var minIndex = i
        for (j in i + 1 until n) {
            if (arr[j] < arr[minIndex]) {
                minIndex = j
            }
        }
        if (minIndex != i) {
            val tmp = arr[i]
            arr[i] = arr[minIndex]
            arr[minIndex] = tmp
        }
    }
}

fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)
    selectionSort(arr)
    println(arr.joinToString())
}
