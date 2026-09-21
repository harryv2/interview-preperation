package lld.amazonlockersimpledelivery.service

import lld.amazonlockersimpledelivery.entity.Customer

interface NotificationService {
    fun notify(customer: Customer, message: String)
}

class ConsoleNotificationService : NotificationService {
    override fun notify(customer: Customer, message: String) {
        println("[sms -> ${customer.phone}] $message")
    }
}
