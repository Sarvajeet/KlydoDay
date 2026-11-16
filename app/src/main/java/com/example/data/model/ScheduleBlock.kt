package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ScheduleCategory(
    val key: String,
    val label: String,
    val defaultColorHex: String
) {
    FOCUS("focus", "Deep Focus", "#E06B3E"),
    WELLNESS("wellness", "Wellness", "#5E9E7A"),
    FAMILY("family", "Family", "#E8A838"),
    MEAL("meal", "Meal & Tea", "#D97D54"),
    CREATIVE("creative", "Creative", "#6B7FE3"),
    REST("rest", "Rest & Wind Down", "#3EA89C"),
    ERRAND("errand", "Home & Errand", "#A078D8");

    companion object {
        fun fromKey(key: String): ScheduleCategory =
            entries.firstOrNull { it.key == key } ?: FOCUS
    }
}

@IgnoreExtraProperties
data class ScheduleBlock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val startMinutes: Long = 540L,
    val durationMinutes: Long = 60L,
    val category: String = ScheduleCategory.FOCUS.key,
    val colorHex: String = ScheduleCategory.FOCUS.defaultColorHex,
    val daysOfWeek: List<String> = emptyList(),
    val chimeEnabled: Boolean = true,
    val completedDateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val endMinutes: Int
        get() = ((startMinutes + durationMinutes) % 1440L).toInt()

    fun isActiveForDay(dayCode: String): Boolean =
        daysOfWeek.isEmpty() || daysOfWeek.contains(dayCode)

    fun isCompletedOn(dateKey: String): Boolean =
        completedDateKey.isNotBlank() && completedDateKey == dateKey
}
