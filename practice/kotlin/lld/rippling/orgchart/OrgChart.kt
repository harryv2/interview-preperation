package lld.rippling.orgchart

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Employee(
    val id: String,
    val name: String,
    var department: String,
) {
    var manager: Employee? = null
    val reports = LinkedHashSet<Employee>()

    override fun toString(): String {
        return name
    }
}

class OrgChart {
    private val employees = HashMap<String, Employee>()
    private val lock = ReentrantReadWriteLock()

    fun add(id: String, name: String, department: String, managerId: String? = null) {
        lock.write {
            require(!employees.containsKey(id)) { "Employee $id exists" }
            val employee = Employee(id, name, department)
            if (managerId != null) {
                attach(employee, find(managerId))
            }
            employees[id] = employee
        }
    }

    fun commonManager(aId: String, bId: String): Employee? {
        lock.read {
            val chainOfA = chainUp(find(aId)).toHashSet()
            return chainUp(find(bId)).firstOrNull { it in chainOfA }
        }
    }

    fun reportingLine(fromId: String, toId: String): List<Employee> {
        lock.read {
            val from = find(fromId)
            val to = find(toId)
            val common = commonManager(fromId, toId) ?: return emptyList()
            val up = chainUp(from).takeWhile { it != common }
            val down = chainUp(to).takeWhile { it != common }.reversed()
            return up + common + down
        }
    }

    fun directReportCount(id: String): Int {
        lock.read {
            return find(id).reports.size
        }
    }

    fun totalReportCount(id: String): Int {
        lock.read {
            return subtree(find(id)).size - 1
        }
    }

    fun indirectReportCount(id: String): Int {
        lock.read {
            return totalReportCount(id) - directReportCount(id)
        }
    }

    fun move(id: String, newManagerId: String, withTeam: Boolean = true, newDepartment: String? = null) {
        lock.write {
            val employee = find(id)
            val newManager = find(newManagerId)
            require(employee.manager != null) { "Cannot move the root" }
            require(employee != newManager) { "Cannot report to self" }

            if (!withTeam) {
                val oldManager = employee.manager!!
                for (report in employee.reports.toList()) {
                    detach(report)
                    attach(report, oldManager)
                }
            }
            require(newManager !in subtree(employee)) { "${newManager.name} reports to ${employee.name}" }

            detach(employee)
            attach(employee, newManager)
            if (newDepartment != null) {
                for (member in subtree(employee)) {
                    member.department = newDepartment
                }
            }
        }
    }

    fun remove(id: String) {
        lock.write {
            val employee = find(id)
            val manager = requireNotNull(employee.manager) { "Cannot remove the root" }
            for (report in employee.reports.toList()) {
                detach(report)
                attach(report, manager)
            }
            detach(employee)
            employees.remove(id)
        }
    }

    fun print(rootId: String) {
        lock.read {
            printTree(find(rootId), 0)
        }
    }

    private fun printTree(employee: Employee, depth: Int) {
        println("  " + "  ".repeat(depth) + "${employee.name} (${employee.department})")
        for (report in employee.reports) {
            printTree(report, depth + 1)
        }
    }

    private fun chainUp(employee: Employee): List<Employee> {
        val chain = ArrayList<Employee>()
        var current: Employee? = employee
        while (current != null) {
            chain += current
            current = current.manager
        }
        return chain
    }

    private fun subtree(root: Employee): List<Employee> {
        val result = ArrayList<Employee>()
        val stack = ArrayDeque(listOf(root))
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            result += current
            stack.addAll(current.reports)
        }
        return result
    }

    private fun attach(employee: Employee, manager: Employee) {
        employee.manager = manager
        manager.reports += employee
    }

    private fun detach(employee: Employee) {
        employee.manager?.reports?.remove(employee)
        employee.manager = null
    }

    private fun find(id: String): Employee {
        return requireNotNull(employees[id]) { "No employee $id" }
    }
}
