package com.example.smartnotifier.ui.main

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotifier.R
import com.example.smartnotifier.core.datastore.AppPrefs
import com.example.smartnotifier.core.datastore.appPrefsDataStore
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dataStore = appContext.appPrefsDataStore
    private val db = DatabaseProvider.get(appContext)
    val rulesRepo = RulesRepository(db)
    private val logDao = db.notificationLogDao()

    private val _ruleUpdateBuffer = MutableSharedFlow<RuleEntity>(replay = 0)

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    val notificationLogs: StateFlow<List<NotificationLogEntity>> = logDao.getLatestLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isShowingLogList = MutableStateFlow(false)
    val isShowingLogList = _isShowingLogList.asStateFlow()

    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()

    fun setNotificationAccessGranted(granted: Boolean) {
        _notificationAccessGranted.value = granted
    }

    init {
        observeRuleUpdates()
    }

     @OptIn(FlowPreview::class)
    private fun observeRuleUpdates() {
        viewModelScope.launch {
            _ruleUpdateBuffer
                .debounce(500.milliseconds)
                .collect { saveRuleSafely(it) }
        }
    }


    private suspend fun saveRuleSafely(rule: RuleEntity) {
        try {
            rulesRepo.update(rule)
        } catch (_: SQLiteConstraintException) {
            _errorMessage.emit(R.string.msg_wrn_dup_same_name.toString())
        } catch (_: Exception) {
            _errorMessage.emit(R.string.msg_err_save_failed.toString())
        }
    }

    fun setShowingLogList(show: Boolean) {
        _isShowingLogList.value = show
    }

    fun addRuleFromLog(log: NotificationLogEntity) {
        viewModelScope.launch {
            try {
                val count = rulesRepo.dao.countSimilarTitles(log.packageName, log.channelId, log.title)
                val newTitle = if (count > 0) "${log.title}($count)" else log.title
                
                val newRule = RuleEntity(
                    packageName = log.packageName,
                    channelId = log.channelId,
                    srhTitle = newTitle,
                    voiceMsg = null,
                    enabled = false
                )
                rulesRepo.insert(newRule)
                setShowingLogList(false)
                _errorMessage.emit("ルールを追加しました: $newTitle")
            } catch (_: Exception) {
                _errorMessage.emit("ルールの追加に失敗しました。")
            }
        }
    }

    private val rulesByOrderNewest = rulesRepo
        .observeAllRulesOrderByNewest()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val rulesByOrderByPackageAsc = rulesRepo
        .observeRulesOrderByPackageAsc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun rules(orderNewest: Boolean = true): StateFlow<List<RuleEntity>> =
        if (orderNewest) rulesByOrderNewest else rulesByOrderByPackageAsc

    suspend fun insert(rule: RuleEntity) = rulesRepo.insert(rule)
    
    fun updateImmediate(rule: RuleEntity) {
        viewModelScope.launch {
            saveRuleSafely(rule)
        }
    }

    fun updateRuleDebounced(rule: RuleEntity) {
        viewModelScope.launch {
            _ruleUpdateBuffer.emit(rule)
        }
    }

    suspend fun delete(rule: RuleEntity) = rulesRepo.delete(rule)
    
    fun duplicateRule(rule: RuleEntity) {
        viewModelScope.launch {
            try {
                rulesRepo.duplicateRule(rule.id)
            } catch (_: Exception) {
                _errorMessage.emit("コピーに失敗しました。")
            }
        }
    }

    val sortList: StateFlow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[AppPrefs.KEY_SORT_LIST_ORDER] ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun updateSortList(orderByPackageAsc: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_SORT_LIST_ORDER] = orderByPackageAsc
            }
        }
    }

    // ⑫: 通知タイトル (デフォルト値をリソースから取得)
    val notificationTitle: StateFlow<String> =
        dataStore.data
            .map { prefs -> 
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] ?: appContext.getString(R.string.default_check_notification_title) 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, appContext.getString(R.string.default_check_notification_title))

    fun updateNotificationTitle(title: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] = title
            }
        }
    }
}
