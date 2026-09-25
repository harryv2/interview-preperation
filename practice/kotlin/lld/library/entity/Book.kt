package lld.library.entity


data class Book(
    val isbn: String,
    val title: String,
    val author: String
) {
    override fun toString(): String {
        return "$title by $author"
    }
}


enum class CopyStatus {
    AVAILABLE,
    LOANED,
    RESERVED
}


class BookCopy(
    val barcode: String,
    val book: Book
) {

    var status: CopyStatus = CopyStatus.AVAILABLE
        private set

    var heldFor: String? = null
        private set

    fun loanOut() {
        check(status != CopyStatus.LOANED) { "Copy $barcode is already loaned" }
        status = CopyStatus.LOANED
        heldFor = null
    }

    fun shelve() {
        status = CopyStatus.AVAILABLE
        heldFor = null
    }

    fun holdFor(memberId: String) {
        status = CopyStatus.RESERVED
        heldFor = memberId
    }

    fun isAvailableTo(memberId: String): Boolean {
        if (status == CopyStatus.AVAILABLE) {
            return true
        }
        return status == CopyStatus.RESERVED && heldFor == memberId
    }

    override fun toString(): String {
        return "$barcode ($book) [$status]"
    }
}
