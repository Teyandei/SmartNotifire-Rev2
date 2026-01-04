package com.example.smartnotifier.core.startup

import android.content.Context
import android.net.Uri
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

// -----------------------------
// App Startup Initializer
// -----------------------------

class DatabaseInitializer : Initializer<Unit> {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun create(context: Context) {
        val appContext = context.applicationContext

        val db: AppDatabase = DatabaseProvider.get(appContext)
        val rulesRepository = RulesRepository(db)

        scope.launch {
            val isFirst = isFirstLaunch(appContext)
            if (isFirst) {
                // 1) 確認用Ruleを追加
                insertInitialCheckRule(appContext, rulesRepository)

                // 2) UI用の初期設定値を保存（⑨・⑫）
                initializeUiDefaults(appContext)

                // 3) 初回起動フラグをオフにする
                markAsLaunched(appContext)
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    // -----------------------------
    // 初回起動フラグ
    // -----------------------------

    private suspend fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.appPrefsDataStore.data.first()
        return prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] ?: true
    }

    private suspend fun markAsLaunched(context: Context) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] = false
        }
    }

    // -----------------------------
    // ⑨, ⑫ の初期値を DataStore に保存
    // -----------------------------

    private suspend fun initializeUiDefaults(context: Context) {
        context.appPrefsDataStore.edit { prefs ->
            // 並び順: 初期値 false = ID降順（設計書のFalseに対応）
            if (!prefs.contains(AppPrefs.KEY_SORT_LIST_ORDER)) {
                prefs[AppPrefs.KEY_SORT_LIST_ORDER] = false
            }

            // 通知タイトル: デフォルト「テスト通知」
            if (!prefs.contains(AppPrefs.KEY_NOTIFICATION_TITLE)) {
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] = AppPrefs.DEFAULT_CHECK_NOTIFICATION_TITLE
            }
        }
    }

    // -----------------------------
    // 初回起動時の確認用 Rule 追加（前回までの実装）
    // -----------------------------

    private suspend fun insertInitialCheckRule(
        context: Context,
        rulesRepository: RulesRepository
    ) {
        val packageName = context.packageName

        val iconUri: Uri = Uri.parse(
            "android.resource://$packageName/${R.mipmap.ic_launcher}"
        )

        val initialRule = RuleEntity(
            id = 0,
            packageName = packageName,
            channelId = "check",
            notificationIcon = iconUri,
            srhTitle = AppPrefs.DEFAULT_CHECK_NOTIFICATION_TITLE,
            voiceMsg = AppPrefs.DEFAULT_CHECK_VOICE_MESSAGE,
            enabled = false
        )

        rulesRepository.insert(initialRule)
    }
}
