package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.KlydoDataSource
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KlydoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : KlydoDataSource {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun blocksCollection(userId: String) =
        db.collection("users").document(userId).collection("schedule_blocks")

    private fun settingsDoc(userId: String) =
        db.collection("users").document(userId).collection("display_settings").document(SETTINGS_DOC_ID)

    override fun observeScheduleBlocks(userId: String): Flow<List<ScheduleBlock>> = flow {
        val path = "users/$userId/schedule_blocks"
        emitAll(
            blocksCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScheduleBlock::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.startMinutes }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    override suspend fun createScheduleBlock(block: ScheduleBlock): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = blocksCollection(uid).document()
            val payload = mapOf(
                "userId" to uid,
                "title" to block.title.trim().take(80),
                "subtitle" to block.subtitle.trim().take(120),
                "startMinutes" to block.startMinutes.coerceIn(0L, 1439L),
                "durationMinutes" to block.durationMinutes.coerceIn(5L, 720L),
                "category" to ScheduleCategory.fromKey(block.category).key,
                "colorHex" to block.colorHex,
                "daysOfWeek" to block.daysOfWeek.take(7),
                "chimeEnabled" to block.chimeEnabled,
                "completedDateKey" to block.completedDateKey.take(16),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/schedule_blocks", auth)
            Result.failure(e)
        }
    }

    override suspend fun updateScheduleBlock(block: ScheduleBlock): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(block.id.isNotBlank()) { "ScheduleBlock ID must not be blank for update" }
            val docRef = blocksCollection(uid).document(block.id)
            val payload = mapOf(
                "title" to block.title.trim().take(80),
                "subtitle" to block.subtitle.trim().take(120),
                "startMinutes" to block.startMinutes.coerceIn(0L, 1439L),
                "durationMinutes" to block.durationMinutes.coerceIn(5L, 720L),
                "category" to ScheduleCategory.fromKey(block.category).key,
                "colorHex" to block.colorHex,
                "daysOfWeek" to block.daysOfWeek.take(7),
                "chimeEnabled" to block.chimeEnabled,
                "completedDateKey" to block.completedDateKey.take(16),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/schedule_blocks/${block.id}", auth)
            Result.failure(e)
        }
    }

    override suspend fun toggleBlockCompleted(blockId: String, completedDateKey: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(blockId.isNotBlank()) { "Block ID must not be blank" }
            val docRef = blocksCollection(uid).document(blockId)
            docRef.update(
                mapOf(
                    "completedDateKey" to completedDateKey.take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/schedule_blocks/$blockId", auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteScheduleBlock(blockId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(blockId.isNotBlank()) { "Block ID must not be blank" }
            val docRef = blocksCollection(uid).document(blockId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/schedule_blocks/$blockId", auth)
            Result.failure(e)
        }
    }

    override fun observeDisplaySettings(userId: String): Flow<DisplaySettings> = flow {
        val path = "users/$userId/display_settings/$SETTINGS_DOC_ID"
        emitAll(
            settingsDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            DisplaySettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: DisplaySettings(id = SETTINGS_DOC_ID, userId = userId)
                    } else {
                        DisplaySettings(id = SETTINGS_DOC_ID, userId = userId)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path, auth)
                    }
                    throw error
                }
        )
    }

    override suspend fun saveDisplaySettings(settings: DisplaySettings): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = settingsDoc(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "activeKlydoId" to settings.activeKlydoId.take(48),
                    "autoCycleArt" to settings.autoCycleArt,
                    "cycleIntervalMinutes" to settings.cycleIntervalMinutes.coerceIn(15L, 1440L),
                    "pendulumStyle" to PendulumStyle.fromKey(settings.pendulumStyle).key,
                    "pendulumEnabled" to settings.pendulumEnabled,
                    "hourlyChimeEnabled" to settings.hourlyChimeEnabled,
                    "blockChimeEnabled" to settings.blockChimeEnabled,
                    "keepScreenOn" to settings.keepScreenOn,
                    "nightModeEnabled" to settings.nightModeEnabled,
                    "nightStartMinutes" to settings.nightStartMinutes.coerceIn(0L, 1439L),
                    "nightEndMinutes" to settings.nightEndMinutes.coerceIn(0L, 1439L),
                    "clockFormat24h" to settings.clockFormat24h,
                    "showFrame" to settings.showFrame,
                    "favoriteKlydoIds" to settings.favoriteKlydoIds.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "activeKlydoId" to settings.activeKlydoId.take(48),
                    "autoCycleArt" to settings.autoCycleArt,
                    "cycleIntervalMinutes" to settings.cycleIntervalMinutes.coerceIn(15L, 1440L),
                    "pendulumStyle" to PendulumStyle.fromKey(settings.pendulumStyle).key,
                    "pendulumEnabled" to settings.pendulumEnabled,
                    "hourlyChimeEnabled" to settings.hourlyChimeEnabled,
                    "blockChimeEnabled" to settings.blockChimeEnabled,
                    "keepScreenOn" to settings.keepScreenOn,
                    "nightModeEnabled" to settings.nightModeEnabled,
                    "nightStartMinutes" to settings.nightStartMinutes.coerceIn(0L, 1439L),
                    "nightEndMinutes" to settings.nightEndMinutes.coerceIn(0L, 1439L),
                    "clockFormat24h" to settings.clockFormat24h,
                    "showFrame" to settings.showFrame,
                    "favoriteKlydoIds" to settings.favoriteKlydoIds.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/display_settings/$SETTINGS_DOC_ID", auth)
            Result.failure(e)
        }
    }

    suspend fun getScheduleBlock(blockId: String): Result<ScheduleBlock?> {
        return try {
            val uid = requireUserId()
            val snapshot = blocksCollection(uid).document(blockId).get().await()
            val block = snapshot.toObject(
                ScheduleBlock::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.GET, "users/$uid/schedule_blocks/$blockId", auth)
            Result.failure(e)
        }
    }

    companion object {
        const val SETTINGS_DOC_ID = "default"
    }
}
