package com.example.smartnotifier.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val rulesRepository = RulesRepository(db)

    private val _rules = MutableStateFlow<List<RuleEntity>>(emptyList())
    val rules: StateFlow<List<RuleEntity>> = _rules.asStateFlow()

    init {
        viewModelScope.launch {
            rulesRepository
                .observeAllRulesOrderByNewest()
                .collect { list ->
                    _rules.value = list
                }
        }
    }
}