package com.netwatch.app.data.local

import androidx.room.TypeConverter
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.NetworkTechnology

class AppTypeConverters {
    @TypeConverter
    fun toTechnology(value: String?): NetworkTechnology? {
        return value?.let(NetworkTechnology::valueOf)
    }

    @TypeConverter
    fun fromTechnology(value: NetworkTechnology?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEventType(value: String): NetworkEventType {
        return NetworkEventType.valueOf(value)
    }

    @TypeConverter
    fun fromEventType(value: NetworkEventType): String {
        return value.name
    }
}
