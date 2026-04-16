package com.nexpos.core.data.model

data class TransactionInfo(
    val id: Int,
    val outletId: Int,
    val customer: String,
    val service: String? = null,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val outletName: String? = null
)

data class CreateTransactionRequest(
    val customer: String,
    val service: String,
    val amount: Double
)

data class ServiceInfo(
    val id: String,
    val outlet_id: String? = null,
    val owner_id: String? = null,
    val name: String,
    val price: Int,
    val unit: String? = null,
    val created_at: String? = null
)

data class ServiceListResponse(
    val services: List<ServiceInfo>
)

data class ServiceResponse(
    val service: ServiceInfo
)

data class CreateServiceRequest(
    val outletId: Int?,
    val name: String,
    val price: Int,
    val unit: String
)

data class CustomerInfo(
    val id: String,
    val outlet_id: String? = null,
    val owner_id: String? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val created_at: String? = null
)

data class CustomerListResponse(
    val customers: List<CustomerInfo>
)

data class CustomerResponse(
    val customer: CustomerInfo
)

data class CreateCustomerRequest(
    val outletId: Int?,
    val name: String,
    val phone: String,
    val address: String
)

data class UpdateStatusRequest(
    val transactionId: Int,
    val status: String
)

data class TransactionListResponse(
    val transactions: List<TransactionInfo>
)
