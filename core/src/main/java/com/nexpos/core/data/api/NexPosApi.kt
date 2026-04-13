package com.nexpos.core.data.api

import com.nexpos.core.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NexPosApi {

    @GET("/api/healthz")
    suspend fun healthCheck(): Response<HealthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/login-device")
    suspend fun loginDevice(@Body request: DeviceLoginRequest): Response<AuthResponse>

    @DELETE("/api/auth/account")
    suspend fun deleteAccount(@Header("Authorization") token: String): Response<MessageResponse>

    @GET("/api/outlets")
    suspend fun getOutlets(@Header("Authorization") token: String): Response<OutletListResponse>

    @POST("/api/outlets")
    suspend fun createOutlet(
        @Header("Authorization") token: String,
        @Body request: CreateOutletRequest
    ): Response<OutletResponse>

    @GET("/api/devices")
    suspend fun getDevices(@Header("Authorization") token: String): Response<DeviceListResponse>

    @POST("/api/devices/heartbeat")
    suspend fun sendHeartbeat(
        @Header("Authorization") token: String,
        @Body request: HeartbeatRequest
    ): Response<MessageResponse>

    @POST("/api/devices/force-logout")
    suspend fun forceLogout(
        @Header("Authorization") token: String,
        @Body request: ForceLogoutRequest
    ): Response<MessageResponse>

    @GET("/api/transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String,
        @Query("outletId") outletId: Int? = null
    ): Response<TransactionListResponse>

    @POST("/api/transactions")
    suspend fun createTransaction(
        @Header("Authorization") token: String,
        @Body request: CreateTransactionRequest
    ): Response<TransactionInfo>

    @PUT("/api/transactions/status")
    suspend fun updateTransactionStatus(
        @Header("Authorization") token: String,
        @Body request: UpdateStatusRequest
    ): Response<TransactionInfo>
}
