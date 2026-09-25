package lld.messaging.oncallalerting

import lld.messaging.oncallalerting.entity.ChannelType
import lld.messaging.oncallalerting.entity.EscalationLevel
import lld.messaging.oncallalerting.entity.EscalationPolicy
import lld.messaging.oncallalerting.entity.Project
import lld.messaging.oncallalerting.entity.Responder
import lld.messaging.oncallalerting.entity.Severity
import lld.messaging.oncallalerting.service.AlertingService
import lld.messaging.oncallalerting.strategy.EmailChannel
import lld.messaging.oncallalerting.strategy.FlakyChannel
import lld.messaging.oncallalerting.strategy.IvrChannel
import lld.messaging.oncallalerting.strategy.SmsChannel
import java.time.Duration

fun main() {

    val asha = Responder("r1", "Asha", "asha@corp.in", "+91-90000-00001")
    val bhavin = Responder("r2", "Bhavin", "bhavin@corp.in", "+91-90000-00002")
    val divya = Responder("r3", "Divya", "divya@corp.in", "+91-90000-00003")
    val eshan = Responder("r4", "Eshan", "eshan@corp.in", "+91-90000-00004")

    // short waits so the demo runs in a second, production numbers are minutes
    val payments = Project(
        id = "PAY",
        name = "Payments",
        policy = EscalationPolicy(
            listOf(
                EscalationLevel(listOf(asha), listOf(ChannelType.EMAIL, ChannelType.SMS), Duration.ofMillis(300)),
                EscalationLevel(listOf(bhavin), listOf(ChannelType.SMS, ChannelType.IVR), Duration.ofMillis(300)),
                EscalationLevel(listOf(divya, eshan), listOf(ChannelType.IVR), Duration.ofMillis(300))
            )
        )
    )

    val search = Project(
        id = "SRCH",
        name = "Search",
        policy = EscalationPolicy(
            listOf(
                EscalationLevel(listOf(divya), listOf(ChannelType.EMAIL), Duration.ofMillis(300)),
                EscalationLevel(listOf(eshan), listOf(ChannelType.IVR), Duration.ofMillis(300))
            )
        )
    )

    val service = AlertingService(
        listOf(EmailChannel(), FlakyChannel(SmsChannel(), failEvery = 3), IvrChannel())
    )
    service.register(payments)
    service.register(search)

    println("== nobody answers, the page climbs the matrix ==")
    val ignored = service.raise("PAY", "Checkout error rate above 5%", Severity.SEV1)
    Thread.sleep(1100)
    println("  $ignored")
    ignored.timeline().forEach { println("    $it") }

    println("\n== level 0 answers, nobody above is ever woken ==")
    val answered = service.raise("PAY", "Refund queue backing up", Severity.SEV2)
    Thread.sleep(100)
    println("  Asha presses 1: ${service.acknowledge(answered.id, asha)}")
    Thread.sleep(700)
    println("  $answered")
    answered.timeline().forEach { println("    $it") }

    println("\n== answered at level 1, after one escalation ==")
    val late = service.raise("PAY", "Settlement job stuck", Severity.SEV1)
    Thread.sleep(400)
    println("  Bhavin answers: ${service.acknowledge(late.id, bhavin)}")
    Thread.sleep(500)
    println("  $late")
    late.timeline().forEach { println("    $it") }

    println("\n== a second acknowledge is late, not an error ==")
    println("  Asha acknowledges too: ${service.acknowledge(late.id, asha)}")
    println("  still ack by ${late.acknowledgedBy}")

    println("\n== each project has its own matrix ==")
    val other = service.raise("SRCH", "Index lag 20 minutes", Severity.SEV3)
    Thread.sleep(400)
    println("  $other")

    println("\n== rejected up front ==")
    attempt("acknowledge by someone not on call for Payments") { service.acknowledge(ignored.id, divya) }
    attempt("raise on an unknown project") { service.raise("NOPE", "?", Severity.SEV3) }
    attempt("a level with nobody on it") { EscalationLevel(emptyList(), listOf(ChannelType.SMS), Duration.ofMinutes(5)) }

    println("\n== resolve ==")
    println("  ${service.resolve(late.id, bhavin)} -> $late")
    println("  open: ${service.open().size}")

    service.shutdown()
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
