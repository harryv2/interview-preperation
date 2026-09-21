package lld.amazonlocker.service

import lld.amazonlocker.entity.Customer

interface NotificationService {
    fun notify(customer: Customer, message: String)
}

class ConsoleNotificationService : NotificationService {
    override fun notify(customer: Customer, message: String) {
        println("[sms -> ${customer.phone}] $message")
    }
}
