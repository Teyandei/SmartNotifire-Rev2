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
