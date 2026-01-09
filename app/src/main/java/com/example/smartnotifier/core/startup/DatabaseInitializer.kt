package com.example.smartnotifier.core.startup

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

class DatabaseInitializer : Initializer<Unit> {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private suspend fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.appPrefsDataStore.data.first()
        return prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] ?: true
    }

    private suspend fun markAsLaunched(context: Context) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[AppPrefs.KEY_IS_FIRST_LAUNCH] = false
        }
    }

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
