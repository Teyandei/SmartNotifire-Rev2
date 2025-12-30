package com.example.smartnotifier.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smartnotifier.data.db.entity.RuleEntity

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRulesDesc(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: RuleEntity)

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)
}