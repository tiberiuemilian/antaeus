package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    IN_PROGRESS,
    ERR_UNAVAILABLE_FUNDS,
    ERR_CUSTOMER_NOT_FOUND,
    ERR_CURRENCY_MISMATCH,
    ERR_NETWORK,
    ERR_UNKNOWN,
    PAID,
    TO_BE_CHECKED; // is PAID or ERR - we couldn't register processing state

    fun inErrors(): Boolean {
        return arrayOf(
            ERR_UNAVAILABLE_FUNDS,
            ERR_CUSTOMER_NOT_FOUND,
            ERR_CURRENCY_MISMATCH,
            ERR_NETWORK,
            ERR_UNKNOWN
        ).contains(this)
    }
}
