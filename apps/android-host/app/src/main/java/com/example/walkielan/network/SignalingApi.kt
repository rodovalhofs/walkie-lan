package com.example.walkielan.network

import com.example.walkielan.data.CreateRoomRequest
import com.example.walkielan.data.JoinRoomRequest
import com.example.walkielan.data.JoinRoomResponse
import com.example.walkielan.data.RoomCodeReservation
import com.example.walkielan.data.WalkieJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SignalingApi(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun createRoom(baseUrl: String, payload: CreateRoomRequest): RoomCodeReservation =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${normalizeBaseUrl(baseUrl)}/api/rooms")
                .post(
                    WalkieJson.instance.encodeToString(payload)
                        .toRequestBody("application/json".toMediaType()),
                )
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(parseError(body))
                }
                WalkieJson.instance.decodeFromString(body)
            }
        }

    suspend fun joinRoom(baseUrl: String, payload: JoinRoomRequest): JoinRoomResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${normalizeBaseUrl(baseUrl)}/api/rooms/join")
                .post(
                    WalkieJson.instance.encodeToString(payload)
                        .toRequestBody("application/json".toMediaType()),
                )
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(parseError(body))
                }
                WalkieJson.instance.decodeFromString(body)
            }
        }

    private fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trimEnd('/')

    private fun parseError(body: String): String {
        return Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?: "Falha ao falar com o servidor."
    }
}

