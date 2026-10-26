package lld.rippling.orgchart

fun main() {
    val org = OrgChart()
    org.add("ceo", "Ceo", "exec")
    org.add("cto", "Cto", "eng", "ceo")
    org.add("cfo", "Cfo", "finance", "ceo")
    org.add("em1", "Em1", "eng", "cto")
    org.add("em2", "Em2", "eng", "cto")
    org.add("dev1", "Dev1", "eng", "em1")
    org.add("dev2", "Dev2", "eng", "em1")
    org.add("dev3", "Dev3", "eng", "em2")
    org.add("acc1", "Acc1", "finance", "cfo")
    org.print("ceo")

    println("-- common manager")
    println("  dev1 + dev3 -> ${org.commonManager("dev1", "dev3")}")
    println("  dev1 + acc1 -> ${org.commonManager("dev1", "acc1")}")
    println("  em1 + dev2  -> ${org.commonManager("em1", "dev2")}")

    println("-- reporting line")
    println("  dev1 to acc1: ${org.reportingLine("dev1", "acc1")}")

    println("-- report counts for cto")
    println("  direct=${org.directReportCount("cto")} indirect=${org.indirectReportCount("cto")} total=${org.totalReportCount("cto")}")

    println("-- move em1 and team under cfo, into finance")
    org.move("em1", "cfo", withTeam = true, newDepartment = "finance")
    org.print("ceo")

    println("-- move em2 alone, its reports stay with cto")
    org.move("em2", "cfo", withTeam = false)
    org.print("ceo")

    println("-- cannot move a manager under their own report")
    runCatching { org.move("cfo", "dev1") }.onFailure { println("  ${it.message}") }

    println("-- removing a manager hands their team up, no orphans")
    org.remove("em1")
    org.print("ceo")
}
