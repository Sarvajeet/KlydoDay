package com.example.data

import com.example.data.model.DisplaySettings
import com.example.data.model.ScheduleBlock
import kotlinx.coroutines.flow.Flow

interface KlydoDataSource {
    fun observeScheduleBlocks(userId: String): Flow<List<ScheduleBlock>>
    suspend fun createScheduleBlock(block: ScheduleBlock): Result<String>
    suspend fun updateScheduleBlock(block: ScheduleBlock): Result<Unit>
    suspend fun toggleBlockCompleted(blockId: String, completedDateKey: String): Result<Unit>
    suspend fun deleteScheduleBlock(blockId: String): Result<Unit>
    fun observeDisplaySettings(userId: String): Flow<DisplaySettings>
    suspend fun saveDisplaySettings(settings: DisplaySettings): Result<Unit>
}
