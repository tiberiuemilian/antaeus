package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    IN_PROGRESS,
    CANCELLED,
    ERR_UNAVAILABLE_FUNDS,
    ERR_CUSTOMER_NOT_FOUND,
    ERR_CURRENCY_MISMATCH,
    ERR_NETWORK,
    ERR_UNKNOWN,
    PAID,
    PAID_OR_CANCELLED;

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
