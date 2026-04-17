package com.nexpos.core.data.model

import com.google.gson.annotations.SerializedName

data class TransactionInfo(
    val id: Int,
    val outletId: Int = 0,
    @SerializedName(value = "customer", alternate = ["customerName"])
    val customer: String,
    @SerializedName(value = "service", alternate = ["serviceName"])
    val service: String? = null,
    @SerializedName(value = "amount", alternate = ["totalAmount"])
    val amount: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val outletName: String? = null
)

data class CreateTransactionRequest(
    val outletId: Int,
    val customerId: String,
    val serviceId: String,
    val quantity: Int
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

data class UpdateServiceRequest(
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

data class UpdateCustomerRequest(
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

data class ReportSummary(
    val totalTransactions: Int = 0,
    val totalIncome: Double = 0.0,
    val totalDiterima: Int = 0,
    val totalDicuci: Int = 0,
    val totalDisetrika: Int = 0,
    val totalSelesai: Int = 0,
    val totalDibatalkan: Int = 0
)

data class DailySummary(
    val date: String,
    val count: Int,
    val income: Double
)

data class DailySummaryResponse(
    val days: List<DailySummary>
)

data class StatusOnlyRequest(
    val status: String
)
