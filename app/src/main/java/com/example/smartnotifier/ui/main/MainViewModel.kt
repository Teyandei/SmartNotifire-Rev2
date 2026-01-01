package com.example.smartnotifier.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.get(application)
    private val rulesRepo = RulesRepository(db)

    val rulesByOrderNewest = rulesRepo
        .observeAllRulesOrderByNewest()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    val rulesByOrderByPackageThenNewest = rulesRepo
        .observeRulesOrderByPackageThenNewest()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    fun rules(orderNewest: Boolean = true) = if (orderNewest) rulesByOrderNewest else rulesByOrderByPackageThenNewest

    suspend fun insert(rule: RuleEntity) = rulesRepo.insert(rule)

    suspend fun update(rule: RuleEntity) = rulesRepo.update(rule)

    suspend fun delete(rule: RuleEntity) = rulesRepo.delete(rule)

}
