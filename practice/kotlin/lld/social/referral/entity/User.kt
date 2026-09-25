package lld.social.referral.entity


data class User(
    val id: String,
    val name: String,
    val referralCode: String
) {
    override fun toString(): String {
        return "$name ($id, code $referralCode)"
    }
}
