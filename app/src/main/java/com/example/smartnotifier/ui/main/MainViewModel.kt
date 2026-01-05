package com.example.smartnotifier.ui.main

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotifier.core.datastore.AppPrefs
import com.example.smartnotifier.core.datastore.appPrefsDataStore
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.repository.RulesRepository
import com.example.smartnotifier.data.repository.NotificationLogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dataStore = appContext.appPrefsDataStore
    private val db = DatabaseProvider.get(appContext)
    private val rulesRepo = RulesRepository(db)
    private val logRepo = NotificationLogRepository(db)

    // 編集中のルールを一時的に保持するバッファ (Debounce用)
    private val _ruleUpdateBuffer = MutableSharedFlow<RuleEntity>(replay = 0)

    init {
        // Debounce を使った自動保存ロジック
        viewModelScope.launch {
            _ruleUpdateBuffer
                .debounce(500) // 0.5秒間入力がなければ実行
                .collect { rule ->
                    rulesRepo.update(rule)
                }
        }
    }

    // --- Notification Logs ---
    val notificationLogs: StateFlow<List<NotificationLogEntity>> =
        logRepo.observeLatestLogs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    fun addRuleFromLog(log: NotificationLogEntity) {
        viewModelScope.launch {
            rulesRepo.insertFromNotificationLog(log)
        }
    }

    // --- Rules 一覧 ---

    private val rulesByOrderNewest = rulesRepo
        .observeAllRulesOrderByNewest()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    private val rulesByOrderByPackageAsc = rulesRepo
        .observeRulesOrderByPackageAsc()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    fun rules(orderNewest: Boolean = true): StateFlow<List<RuleEntity>> =
        if (orderNewest) rulesByOrderNewest else rulesByOrderByPackageAsc

    suspend fun insert(rule: RuleEntity) = rulesRepo.insert(rule)
    
    /**
     * 即時更新 (スイッチ切り替えやフォーカスロスト用)
     */
    suspend fun update(rule: RuleEntity) = rulesRepo.update(rule)

    /**
     * バッファ経由の更新 (テキスト入力中用)
     */
    fun updateRuleDebounced(rule: RuleEntity) {
        viewModelScope.launch {
            _ruleUpdateBuffer.emit(rule)
        }
    }

    suspend fun delete(rule: RuleEntity) = rulesRepo.delete(rule)
    
    /**
     * 設計書 ⑤ コピー (トランザクション使用)
     */
    fun duplicateRule(rule: RuleEntity) {
        viewModelScope.launch {
            rulesRepo.duplicateRule(rule.id)
        }
    }

    // --- ⑨: 並び順 sortList ---

    val sortList: StateFlow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[AppPrefs.KEY_SORT_LIST_ORDER] ?: false }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                false
            )

    fun updateSortList(orderByPackageAsc: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_SORT_LIST_ORDER] = orderByPackageAsc
            }
        }
    }

    // --- ⑫: 通知タイトル ntfTitle ---

    val notificationTitle: StateFlow<String> =
        dataStore.data
            .map { prefs -> prefs[AppPrefs.KEY_NOTIFICATION_TITLE] ?: AppPrefs.DEFAULT_CHECK_NOTIFICATION_TITLE }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                AppPrefs.DEFAULT_CHECK_NOTIFICATION_TITLE
            )

    fun updateNotificationTitle(title: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefs.KEY_NOTIFICATION_TITLE] = title
            }
        }
    }
}
