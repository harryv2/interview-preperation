package dsa.mycodeschool.sorting


fun merge(left: IntArray, right: IntArray, arr: IntArray) {
    var i = 0
    var j = 0
    var k = 0

    while (i < left.size && j < right.size) {
        if (left[i] <= right[j]) {
            arr[k] = left[i]
            i++
        } else {
            arr[k] = right[j]
            j++
        }
        k++
    }

    while (i<left.size) {
        arr[k] = left[i]
        i++
        k++
    }

    while (j<right.size) {
        arr[k] = right[j]
        j++
        k++
    }
}

fun mergeSort(arr: IntArray) {
    if (arr.size < 2) return

    var n = arr.size

    var start = 0;
    var mid = n / 2

    var left = IntArray(mid - start)
    var right = IntArray(n - mid )

    for (i in 0..<mid) {
        left[i] = arr[i]
    }

    for (j in mid..<n) {
        right[j - mid] = arr[j]
    }


    mergeSort(left)
    mergeSort(right)
    merge(left, right, arr)
}


fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)
    mergeSort(arr)
    println(arr.joinToString())
}
