package com.aterminal.app.data

import androidx.room.TypeConverter
import com.aterminal.app.agents.AgentType

class EnumConverters {
    @TypeConverter
    fun authTypeFromStorage(value: String): AuthType {
        return AuthType.valueOf(value)
    }

    @TypeConverter
    fun authTypeToStorage(value: AuthType): String {
        return value.name
    }

    @TypeConverter
    fun agentTypeFromStorage(value: String): AgentType {
        return AgentType.valueOf(value)
    }

    @TypeConverter
    fun agentTypeToStorage(value: AgentType): String {
        return value.name
    }
}
