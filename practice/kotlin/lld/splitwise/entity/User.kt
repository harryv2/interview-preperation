package lld.splitwise.entity

// =====================================================================
//  USER
//
//  `id` exists because User is used as a map key. Names repeat and
//  change; ids don't.
// =====================================================================

data class User(
    val id: String,
    val name: String,
    val email: String = ""
) {
    override fun toString(): String = name
}
