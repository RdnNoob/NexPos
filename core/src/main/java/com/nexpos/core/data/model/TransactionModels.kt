package com.nexpos.core.data.model

data class TransactionInfo(
    val id: Int,
    val outletId: Int,
    val customer: String,
    val service: String? = null,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val outletName: String? = null
)

data class CreateTransactionRequest(
    val customer: String,
    val service: String,
    val amount: Double
)

data class UpdateStatusRequest(
    val transactionId: Int,
    val status: String
)

data class TransactionListResponse(
    val transactions: List<TransactionInfo>
)
