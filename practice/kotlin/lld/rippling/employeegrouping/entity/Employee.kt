package lld.rippling.employeegrouping.entity

data class Employee(
    val id: String,
    val name: String,
    val department: String,
    val location: String,
    val title: String,
    val salary: Double,
    val age: Int,
)
