package lld.commerce.amazonlockersimpledelivery.entity

enum class Size {
    SMALL,
    MEDIUM,
    LARGE;

    fun fits(packageSize: Size): Boolean {
        return ordinal >= packageSize.ordinal
    }
}
