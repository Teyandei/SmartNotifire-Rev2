package com.example.smartnotifier.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object AppPrefs {
    const val NAME = "app_prefs"

    // 初回起動
    val KEY_IS_FIRST_LAUNCH: Preferences.Key<Boolean> =
        booleanPreferencesKey("is_first_launch")

    // ⑨ 並び順
    val KEY_SORT_LIST_ORDER: Preferences.Key<Boolean> =
        booleanPreferencesKey("sort_list_order")

    // ⑫ 通知タイトル
    val KEY_NOTIFICATION_TITLE: Preferences.Key<String> =
        stringPreferencesKey("notification_title")

    const val DEFAULT_CHECK_NOTIFICATION_TITLE = "テスト通知"

    const val DEFAULT_CHECK_VOICE_MESSAGE = "通知確認が成功しました。"
}

// ★ ここが “唯一の” DataStore 定義になる
val Context.appPrefsDataStore by preferencesDataStore(
    name = AppPrefs.NAME
)