package com.nexpos.core.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class DeviceLoginRequest(
    val activationCode: String,
    val deviceName: String,
    val deviceId: String
)

data class AuthResponse(
    val token: String,
    val user: UserInfo? = null,
    val outlet: OutletInfo? = null,
    val device: DeviceInfo? = null
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String
)

data class OutletInfo(
    val id: Int,
    val name: String,
    val ownerId: String,
    val activationCode: String? = null
)

data class DeviceInfo(
    val id: Int,
    val deviceName: String,
    val deviceId: String,
    val status: String,
    val outletId: Int,
    val lastSeen: String? = null,
    val outletName: String? = null
)
