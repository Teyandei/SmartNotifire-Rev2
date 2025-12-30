package com.example.smartnotifier.data.db

import android.net.Uri
import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringToUri(value: String?): Uri? =
        value?.let { Uri.parse(it) }

    @TypeConverter
    fun fromUriToString(uri: Uri?): String? =
        uri?.toString()
}