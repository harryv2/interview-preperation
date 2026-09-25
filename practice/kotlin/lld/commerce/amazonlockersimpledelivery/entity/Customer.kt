package lld.commerce.amazonlockersimpledelivery.entity

import kotlin.uuid.Uuid

data class Customer(
    val id: Uuid,
    val name: String,
    val phone: String,
)
