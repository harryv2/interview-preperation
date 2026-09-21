package lld.amazonlockersimple.service

import lld.amazonlockersimple.entity.Customer

interface NotificationService {
    fun notify(customer: Customer, message: String)
}

class ConsoleNotificationService : NotificationService {
    override fun notify(customer: Customer, message: String) {
        println("[sms -> ${customer.phone}] $message")
    }
}
