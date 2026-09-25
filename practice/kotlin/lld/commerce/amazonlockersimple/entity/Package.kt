package lld.commerce.amazonlockersimple.entity

import kotlin.uuid.Uuid

data class Package(
    val id: Uuid,
    val orderId: String,
    val size: Size,
    val customer: Customer,
)
