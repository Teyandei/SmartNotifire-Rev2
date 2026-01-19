package com.example.smartnotifier.core.startup

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
import androidx.datastore.preferences.core.edit
import androidx.startup.Initializer
import com.example.smartnotifier.R
import com.example.smartnotifier.core.datastore.AppPrefs
import com.example.smartnotifier.core.datastore.appPrefsDataStore
import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * アプリケーションの初回起動時にデータベースの初期設定を行うための[Initializer]です。
 *
 * [androidx.startup]ライブラリを利用して、メインスレッドをブロックすることなく、
 * 非同期で初期データの投入やデフォルト設定の保存を実行します。
 *
 * 設計書「機能：通知確認機能」の「インストール後初回起動時の通知検出ルール」に基づき、
 * アプリの動作確認を容易にするための初期ルールをデータベースに挿入します。
 */
class DatabaseInitializer : Initializer<Unit> {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * [Initializer]のメイン処理。
     *
     * 初回起動かどうかを判定し、初回起動であれば初期データのセットアップ処理を非同期で開始します。
     *
     * @param context アプリケーションコンテキスト。
     */
    override fun create(context: Context) {
        val appContext = context.applicationContext
        val db: AppDatabase = DatabaseProvider.get(appContext)
        val rulesRepository = RulesRepository(db)

        scope.launch {
            if (isFirstLaunch(appContext)) {
                insertInitialCheckRule(appContext, rulesRepository)
                initializeUiDefaults(appContext)
                markAsLaunched(appContext)
            }
        }
    }

    /**
     * このInitializerの依存関係を定義します。
     *
     * 今回は依存する他のInitializerがないため、空のリストを返します。
     *
     * @return 依存する[Initializer]のクラスリスト。
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    /**
     * [androidx.datastore.preferences.DataStore]をチェックし、アプリが初回起動かどうかを判定します。
     *
     * @param context アプリケーションコンテキスト。
     * @return 初回起動の場合は`true`、それ以外は`false`。
     */
    private suspend fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.appPrefsDataStore.data.first()
        return prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] ?: true
    }

    /**
     * アプリが起動されたことを記録するため、[DataStore]にフラグを書き込みます。
     *
     * @param context アプリケーションコンテキスト。
     */
    private suspend fun markAsLaunched(context: Context) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] = false
        }
    }

    /**
     * UIに関連する設定のデフォルト値を[DataStore]に保存します。
     *
     * 設計書「⑨ 並び順」「⑫ 通知タイトル」の初期値を設定します。
     *
     * @param context アプリケーションコンテキスト。
     */
    private suspend fun initializeUiDefaults(context: Context) {
        context.appPrefsDataStore.edit { prefs ->
            if (!prefs.contains(AppPrefs.KEY_SORT_LIST_ORDER)) {
                prefs[AppPrefs.KEY_SORT_LIST_ORDER] = false
            }
            if (!prefs.contains(AppPrefs.KEY_NOTIFICATION_TITLE)) {
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] = context.getString(R.string.default_check_notification_title)
            }
        }
    }

    /**
     * 設計書「機能：通知確認機能」に基づき、動作確認用の初期ルールをデータベースに挿入します。
     *
     * このルールは、アプリ自身のテスト通知を検出するために使用されます。
     *
     * @param context アプリケーションコンテキスト。
     * @param rulesRepository ルールをデータベースに挿入するためのリポジトリ。
     */
    private suspend fun insertInitialCheckRule(
        context: Context,
        rulesRepository: RulesRepository
    ) {
        val packageName = context.packageName
        val initialRule = RuleEntity(
            id = 0,
            packageName = packageName,
            channelId = "check",
            srhTitle = context.getString(R.string.default_check_notification_title),
            voiceMsg = context.getString(R.string.default_check_voice_message),
            enabled = false
        )

        rulesRepository.insert(initialRule)
    }
}
