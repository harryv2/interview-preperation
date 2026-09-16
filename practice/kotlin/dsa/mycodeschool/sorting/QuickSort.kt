package dsa.mycodeschool.sorting


fun swap(arr: IntArray,i: Int, j: Int) {
    var temp = arr[i]
    arr[i] = arr[j]
    arr[j] = temp
}


fun partition(arr: IntArray, start: Int, end: Int): Int {
    val pivot = arr[end]
    var pIndex = start

    for(i in start..<end) {
        if(arr[i] <= pivot){
            swap(arr, i, pIndex)
            pIndex++
        }
    }

    swap(arr, pIndex, end)
    return pIndex
}


fun quickSortActual(arr: IntArray, start: Int, end: Int) {
    if (start >= end) {
        return
    }

    var pIndex = partition(arr, start, end)

    quickSortActual(arr, pIndex + 1, end)
    quickSortActual(arr, start, pIndex - 1)
}

fun quickSort(arr: IntArray) {
    quickSortActual(arr, 0, arr.lastIndex)
}


fun main() {
    val arr = intArrayOf(5, 1, 4, 2, 8, 0, 2)
    quickSort(arr)
    println(arr.joinToString())
}
