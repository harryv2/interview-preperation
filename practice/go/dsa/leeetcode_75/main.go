package main

import "fmt"

func swap(nums []int, i, j int) {
	nums[i], nums[j] = nums[j], nums[i]
}

func moveZeroes(nums []int) {
	if len(nums) <= 1 {
		return
	}

	i := 0
	j := 1

	for i < len(nums) && j < len(nums) {
		if nums[i] != 0 {
			i++
			j++
		} else {
			if nums[j] != 0 {
				swap(nums, i, j)
			} else {
				j++
			}
		}
	}

}

func main() {
	arr := []int{0, 1, 6, 0, 9, 8, 0}
	/// [1,3,12,0,0]
	moveZeroes(arr)

	fmt.Println(arr)
}
