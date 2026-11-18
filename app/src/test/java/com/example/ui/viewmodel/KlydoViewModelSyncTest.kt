package com.example.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.data.KlydoDataSource
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class KlydoViewModelSyncTest {

    private class FakeKlydoDataSource(
        emitInitialSettingsImmediately: Boolean = true,
        private val saveDelayMs: Long = 0L
    ) : KlydoDataSource {
        private val blocksFlow = MutableStateFlow<List<ScheduleBlock>>(emptyList())
        private val settingsFlow = MutableStateFlow<DisplaySettings?>(
            if (emitInitialSettingsImmediately) {
                DisplaySettings(id = "default", userId = "guest", showFrame = true, clockFormat24h = true)
            } else {
                null
            }
        )

        val savedSettingsHistory = mutableListOf<DisplaySettings>()

        fun emitInitialSettings(settings: DisplaySettings) {
            settingsFlow.value = settings
        }

        override fun observeScheduleBlocks(userId: String): Flow<List<ScheduleBlock>> =
            blocksFlow.asStateFlow()

        override suspend fun createScheduleBlock(block: ScheduleBlock): Result<String> {
            val id = "blk_${UUID.randomUUID().toString().take(8)}"
            blocksFlow.value = blocksFlow.value + block.copy(id = id, userId = "guest")
            return Result.success(id)
        }

        override suspend fun updateScheduleBlock(block: ScheduleBlock): Result<Unit> {
            blocksFlow.value = blocksFlow.value.map { if (it.id == block.id) block else it }
            return Result.success(Unit)
        }

        override suspend fun toggleBlockCompleted(blockId: String, completedDateKey: String): Result<Unit> {
            blocksFlow.value = blocksFlow.value.map {
                if (it.id == blockId) it.copy(completedDateKey = completedDateKey) else it
            }
            return Result.success(Unit)
        }

        override suspend fun deleteScheduleBlock(blockId: String): Result<Unit> {
            blocksFlow.value = blocksFlow.value.filterNot { it.id == blockId }
            return Result.success(Unit)
        }

        override fun observeDisplaySettings(userId: String): Flow<DisplaySettings> =
            settingsFlow.filterNotNull()

        override suspend fun saveDisplaySettings(settings: DisplaySettings): Result<Unit> {
            if (saveDelayMs > 0L) delay(saveDelayMs)
            savedSettingsHistory += settings
            settingsFlow.value = settings
            return Result.success(Unit)
        }

        fun currentBlocks(): List<ScheduleBlock> = blocksFlow.value
        fun currentSettings(): DisplaySettings? = settingsFlow.value
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun managedVm(repo: KlydoDataSource): Pair<KlydoViewModel, ViewModelStore> {
        val store = ViewModelStore()
        val vm = KlydoViewModel(
            repository = repo,
            currentUserId = "guest",
            appContext = null,
            enableTicker = false
        )
        store.put("test_vm", vm)
        return vm to store
    }

    @Test
    fun hasLoadedSettingsRemainsFalseUntilRepositoryEmitsInitialSettings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeKlydoDataSource(emitInitialSettingsImmediately = false)
        val (vm, store) = managedVm(repo)
        try {
            advanceUntilIdle()
            assertFalse(vm.hasLoadedSettings.value)

            val persisted = DisplaySettings(
                id = "default",
                userId = "guest",
                activeKlydoId = "zen_ripple",
                showFrame = true,
                clockFormat24h = true
            )
            repo.emitInitialSettings(persisted)
            advanceUntilIdle()

            assertTrue(vm.hasLoadedSettings.value)
            assertEquals("zen_ripple", vm.displaySettingsState.value.activeKlydoId)
            assertTrue(vm.displaySettingsState.value.showFrame)
            assertTrue(vm.displaySettingsState.value.clockFormat24h)
        } finally {
            store.clear()
        }
    }

    @Test
    fun rapidConcurrentSettingsMutationsComposeAtomicallyWithoutClobbering() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeKlydoDataSource(emitInitialSettingsImmediately = false, saveDelayMs = 25L)
        val (vm, store) = managedVm(repo)
        try {
            repo.emitInitialSettings(
                DisplaySettings(
                    id = "default",
                    userId = "guest",
                    activeKlydoId = "bioluminescent_jelly",
                    showFrame = false,
                    clockFormat24h = false,
                    favoriteKlydoIds = listOf("bioluminescent_jelly")
                )
            )
            advanceUntilIdle()
            assertTrue(vm.hasLoadedSettings.value)

            // Fire 4 concurrent updates back-to-back while saveDisplaySettings suspends
            vm.setShowFrame(true)
            vm.updateDisplaySettings { it.copy(clockFormat24h = true) }
            vm.toggleFavoriteKlydo("bauhaus_orbit")
            vm.selectPendulumStyle(PendulumStyle.BRASS)
            advanceUntilIdle()

            assertEquals(4, repo.savedSettingsHistory.size)
            val finalSaved = repo.currentSettings()!!
            assertTrue("showFrame must be preserved", finalSaved.showFrame)
            assertTrue("clockFormat24h must be preserved", finalSaved.clockFormat24h)
            assertTrue(finalSaved.favoriteKlydoIds.contains("bauhaus_orbit"))
            assertEquals(PendulumStyle.BRASS.key, finalSaved.pendulumStyle)
        } finally {
            store.clear()
        }
    }

    @Test
    fun scheduleCrudStarterRoutineAndScrubActionsUpdateStateCorrectly() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeKlydoDataSource(emitInitialSettingsImmediately = true)
        val (vm, store) = managedVm(repo)
        try {
            advanceUntilIdle()

            vm.addStarterRoutine()
            advanceUntilIdle()
            assertEquals(5, repo.currentBlocks().size)

            vm.saveBlock(ScheduleBlock(title = "Reading", startMinutes = 1200L, durationMinutes = 30L))
            advanceUntilIdle()
            assertEquals(6, repo.currentBlocks().size)
            val created = repo.currentBlocks().first { it.title == "Reading" }

            vm.toggleBlockCompletedToday(created)
            advanceUntilIdle()
            val today = KlydoViewModel.currentDateKey()
            assertTrue(repo.currentBlocks().first { it.id == created.id }.isCompletedOn(today))

            vm.deleteBlock(created.id)
            advanceUntilIdle()
            assertFalse(repo.currentBlocks().any { it.id == created.id })

            vm.adjustScrubMinutes(900f)
            assertEquals(720f, vm.scrubOffsetMinutes.value, 0.01f)
            vm.snapToLiveTime()
            assertEquals(0f, vm.scrubOffsetMinutes.value, 0.01f)
        } finally {
            store.clear()
        }
    }
}
