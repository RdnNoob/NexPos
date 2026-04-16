package com.nexpos.core.data.model

data class SuperAdminLoginRequest(
    val username: String,
    val password: String
)

data class SuperAdminLoginResponse(
    val token: String,
    val username: String
)

data class SuperAdminStatsResponse(
    val totalUsers: Int,
    val totalOutlets: Int,
    val totalDevices: Int,
    val totalTransactions: Int,
    val totalOtpRequests: Int
)

data class SuperAdminUser(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: String? = null,
    val accountStatus: String = "active",
    val penaltyReason: String? = null,
    val bannedPermanent: Boolean = false,
    val outletCount: Int = 0,
    val deviceCount: Int = 0
)

data class SuperAdminUsersResponse(
    val users: List<SuperAdminUser>
)

data class OtpRequestInfo(
    val id: Int,
    val userId: String? = null,
    val email: String,
    val name: String? = null,
    val otpCode: String,
    val message: String? = null,
    val createdAt: String? = null
)

data class OtpRequestsResponse(
    val requests: List<OtpRequestInfo>
)

data class BanUserRequest(
    val reason: String,
    val permanent: Boolean = true
)

data class CreateNotificationRequest(
    val ownerId: String,
    val title: String,
    val message: String,
    val type: String = "info"
)

data class NotificationInfo(
    val id: Int,
    val title: String,
    val message: String,
    val type: String = "info",
    val isRead: Boolean = false,
    val createdAt: String? = null
)

data class NotificationListResponse(
    val notifications: List<NotificationInfo>
)