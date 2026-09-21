package dsa.mycodeschool.fenwicktree

class FenwickTree(
    arr: LongArray
) {

    val tree = longArrayOf(-1) + arr.copyOf()

    init {
        val n = tree.size
        for (i in 1..n) {
            val j = i + Integer.lowestOneBit(i)
            if(j < n) {
                tree[j] += tree[i]
            }
        }
    }


    fun prefixSum(i: Int): Long {
        var sum = 0L
        var li = i;
        while (li > 0) {
            sum += tree[li]
            li -= Integer.lowestOneBit(li)
        }
        return sum
    }

    fun sum(i: Int, j: Int): Long {
        require(j >= i) {"j >= i required"}
        return prefixSum(j) - prefixSum(i - 1)
    }

    fun add(pI: Int, k: Long) {
        var i = pI;
        while (i < tree.size) {
            tree[i] += k
            i += Integer.lowestOneBit(i)
        }
    }

    fun set(i: Int, k: Long) {
        val value = sum(i, i)
        add(i,  k - value)
    }

    fun prefixSumZeroBased(i: Int): Long = prefixSum(i + 1)

    fun sumZeroBased(i: Int, j: Int): Long = sum(i + 1, j + 1)

    fun addZeroBased(i: Int, k: Long) = add(i + 1, k)

    fun setZeroBased(i: Int, k: Long) = set(i + 1, k)

    override fun toString(): String {
        return tree.joinToString(", ")
    }



}


// LeetCode 307. Range Sum Query - Mutable
class NumArray(nums: IntArray) {

    private val tree = FenwickTree(LongArray(nums.size) { nums[it].toLong() })

    fun update(index: Int, value: Int) {
        tree.setZeroBased(index, value.toLong())
    }

    fun sumRange(left: Int, right: Int): Int {
        return tree.sumZeroBased(left, right).toInt()
    }
}


// LeetCode 315. Count of Smaller Numbers After Self
fun countSmaller(nums: IntArray): List<Int> {
    val sorted = nums.toSortedSet().toIntArray()
    val tree = FenwickTree(LongArray(sorted.size))
    val result = IntArray(nums.size)

    for (i in nums.indices.reversed()) {
        val rank = sorted.binarySearch(nums[i]) + 1
        result[i] = tree.prefixSum(rank - 1).toInt()
        tree.add(rank, 1)
    }

    return result.toList()
}


fun main() {

    val ft = FenwickTree(longArrayOf(3, 2, -1, 6, 5, 4, -3, 3, 7, 2, 3))

    println(ft.sum(3, 6))            // 14, -1 + 6 + 5 + 4
    println(ft.sumZeroBased(2, 5))   // 14, same range zero based

    ft.addZeroBased(3, 4)            // 6 -> 10
    println(ft.sumZeroBased(2, 5))   // 18

    ft.setZeroBased(2, 0)            // -1 -> 0
    println(ft.sumZeroBased(2, 5))   // 19


    val numArray = NumArray(intArrayOf(1, 3, 5))

    println(numArray.sumRange(0, 2))  // 9
    numArray.update(1, 2)
    println(numArray.sumRange(0, 2))  // 8


    println(countSmaller(intArrayOf(5, 2, 6, 1)))    // [2, 1, 1, 0]
    println(countSmaller(intArrayOf(-1, -1)))        // [0, 0]

}
