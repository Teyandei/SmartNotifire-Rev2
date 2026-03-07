package com.example.smartnotifier.core.datastore

/*
 * SmartNotifier-Rev2
 * Copyright (C) 2026  Takeaki Yoshizawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * アプリケーションのプリファレンス定義
 *
 * @property NAME プリファレンスファイル名
 * @property KEY_IS_FIRST_LAUNCH 初回起動フラグ
 * @property KEY_SORT_LIST_ORDER リスト表示順
 * @property KEY_NOTIFICATION_TITLE 通知タイトル
 * @property KEY_TYPE_LOG_ORDER 通知ログ表示順
 * @property SortOrder 並び順
 *
 */
object AppPrefs {
    const val NAME = "app_prefs"

    enum class SortOrder {
        ORDER_BY_NEWEST,    // 新着順
        ORDER_BY_NAME,      // 名称順
        ORDER_BY_COUNT;     // 回数順　*回数等の大きい順
        companion object {
            fun fromOrdinal(ord: Int): SortOrder {
                // Kotlin 1.9+ なら entries が使える
                return entries.getOrNull(ord) ?: ORDER_BY_NEWEST
            }
        }
    }

    // 初回起動
    val KEY_IS_FIRST_LAUNCH: Preferences.Key<Boolean> =
        booleanPreferencesKey("is_first_launch")

    // ⑨ 並び順
    val KEY_SORT_LIST_ORDER: Preferences.Key<Boolean> =
        booleanPreferencesKey("sort_list_order")

    // ⑫ 通知タイトル
    val KEY_NOTIFICATION_TITLE: Preferences.Key<String> =
        stringPreferencesKey("notification_title")

    // 通知ログ表示順
    val KEY_TYPE_LOG_ORDER: Preferences.Key<Int> =
        intPreferencesKey("type_log_order")
}

val Context.appPrefsDataStore by preferencesDataStore(
    name = AppPrefs.NAME
)

