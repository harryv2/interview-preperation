package dsa.leetcode_75


fun getFirstPosition(nums: IntArray, target: Int): Int {
    var start = 0
    var end = nums.lastIndex

    var lastSeenIndex = -1

    while (start <= end) {
        var mid = (start+end)/2

        if(nums[mid] == target) {
            lastSeenIndex = mid
            end = mid - 1
        } else if (nums[mid] > target) {
            end = mid - 1
        } else {
            start = mid + 1
        }
    }

    return lastSeenIndex
}

fun getLastPosition(nums: IntArray, target: Int): Int {
    var start = 0
    var end = nums.lastIndex

    var lastSeenIndex = -1

    while (start <= end) {
        var mid = (start+end)/2

        if(nums[mid] == target) {
            lastSeenIndex = mid
            start = mid + 1
        } else if (nums[mid] > target) {
            end = mid - 1
        } else {
            start = mid + 1
        }
    }

    return lastSeenIndex
}

fun searchRange(nums: IntArray, target: Int): IntArray {

    return intArrayOf(
        getFirstPosition(nums, target),
        getLastPosition(nums, target)
    )
}


fun search(nums: IntArray, target: Int): Int {
    var start = 0
    var end = nums.lastIndex

    while (start <= end) {
        var mid = (start + (end - start)/2)

        if(nums[mid] == target) {
            return mid
        } else {
            var first = nums[start]
            var last = nums[end]
            var midElem = nums[mid]
            if(midElem > last) {
                if(target > midElem) {
                    start = mid + 1
                } else {
                    if(target < midElem && target < last) {
                        start = mid + 1
                    } else {
                        end = mid - 1
                    }
                }
            } else {
                if(target < midElem) {
                    end = mid - 1
                } else {
                    if(target > midElem && target <= last) {
                        start = mid + 1
                    } else {
                        end = mid - 1
                    }
                }
            }
        }
    }

    return -1
}

fun main() {
    print(search(intArrayOf(4,5,6,7,0,1,2), 0))
}