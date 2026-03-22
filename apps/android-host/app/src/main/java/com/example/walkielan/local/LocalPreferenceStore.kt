package com.example.walkielan.local

import android.content.Context

data class SavedPreferences(
    val nickname: String,
    val roomName: String,
    val channelsInput: String,
    val advancedServerUrl: String,
    val lastJoinedRoomCode: String,
)

class LocalPreferenceStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("walkie-lan-prefs", Context.MODE_PRIVATE)

    fun load(): SavedPreferences {
        return SavedPreferences(
            nickname = prefs.getString(KEY_NICKNAME, "Operador Android").orEmpty(),
            roomName = prefs.getString(KEY_ROOM_NAME, "Equipe LAN").orEmpty(),
            channelsInput = prefs.getString(KEY_CHANNELS, "Geral, Operacao, Suporte").orEmpty(),
            advancedServerUrl = prefs.getString(KEY_SERVER_URL, "").orEmpty(),
            lastJoinedRoomCode = prefs.getString(KEY_LAST_ROOM_CODE, "").orEmpty(),
        )
    }

    fun saveNickname(value: String) {
        prefs.edit().putString(KEY_NICKNAME, value).apply()
    }

    fun saveRoomName(value: String) {
        prefs.edit().putString(KEY_ROOM_NAME, value).apply()
    }

    fun saveChannels(value: String) {
        prefs.edit().putString(KEY_CHANNELS, value).apply()
    }

    fun saveAdvancedServerUrl(value: String) {
        prefs.edit().putString(KEY_SERVER_URL, value).apply()
    }

    fun saveLastJoinedRoomCode(value: String) {
        prefs.edit().putString(KEY_LAST_ROOM_CODE, value).apply()
    }

    companion object {
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_ROOM_NAME = "room_name"
        private const val KEY_CHANNELS = "channels_input"
        private const val KEY_SERVER_URL = "advanced_server_url"
        private const val KEY_LAST_ROOM_CODE = "last_joined_room_code"
    }
}
