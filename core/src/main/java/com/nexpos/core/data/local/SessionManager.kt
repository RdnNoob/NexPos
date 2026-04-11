package com.nexpos.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexpos_session")

class SessionManager(private val context: Context) {

    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_ID_KEY = intPreferencesKey("user_id")
        val OUTLET_ID_KEY = intPreferencesKey("outlet_id")
        val OUTLET_NAME_KEY = stringPreferencesKey("outlet_name")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val DEVICE_DB_ID_KEY = intPreferencesKey("device_db_id")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val outletNameFlow: Flow<String?> = context.dataStore.data.map { it[OUTLET_NAME_KEY] }

    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN_KEY]
    suspend fun getOutletId(): Int? = context.dataStore.data.first()[OUTLET_ID_KEY]
    suspend fun getDeviceId(): String? = context.dataStore.data.first()[DEVICE_ID_KEY]

    fun getBearerToken(): Flow<String?> = context.dataStore.data.map {
        it[TOKEN_KEY]?.let { token -> "Bearer $token" }
    }

    // FIX: Persists device ID so the same device doesn't register as new every session
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID_KEY]
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID_KEY] = newId }
        return newId
    }

    suspend fun saveAdminSession(token: String, user: com.nexpos.core.data.model.UserInfo) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[USER_NAME_KEY] = user.name
            it[USER_EMAIL_KEY] = user.email
            it[USER_ID_KEY] = user.id
        }
    }

    suspend fun saveDeviceSession(
        token: String,
        outlet: com.nexpos.core.data.model.OutletInfo?,
        device: com.nexpos.core.data.model.DeviceInfo?,
        deviceIdLocal: String
    ) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            outlet?.let { o ->
                it[OUTLET_ID_KEY] = o.id
                it[OUTLET_NAME_KEY] = o.name
            }
            it[DEVICE_ID_KEY] = deviceIdLocal
            device?.let { d ->
                it[DEVICE_DB_ID_KEY] = d.id
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            // FIX: Keep device_id persisted across logout so device stays consistent
            val savedDeviceId = prefs[DEVICE_ID_KEY]
            prefs.clear()
            savedDeviceId?.let { prefs[DEVICE_ID_KEY] = it }
        }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null
}
