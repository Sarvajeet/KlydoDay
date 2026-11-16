package com.example.data.model

data class VideoFacePalettePreset(
    val key: String,
    val label: String,
    val isDarkFace: Boolean,
    val handsColorHex: String,
    val pendulumRodColorHex: String,
    val pendulumBobColorHex: String
)

object VideoFacePalettes {
    val all: List<VideoFacePalettePreset> = listOf(
        VideoFacePalettePreset(
            key = "cyan_prism",
            label = "Prism Cyan",
            isDarkFace = true,
            handsColorHex = "#28F7E6",
            pendulumRodColorHex = "#28F7E6",
            pendulumBobColorHex = "#9D00DF"
        ),
        VideoFacePalettePreset(
            key = "warm_pearl",
            label = "Champagne Pearl",
            isDarkFace = true,
            handsColorHex = "#F7F3EA",
            pendulumRodColorHex = "#D6C3A5",
            pendulumBobColorHex = "#E5B869"
        ),
        VideoFacePalettePreset(
            key = "terracotta_warm",
            label = "Terracotta Glow",
            isDarkFace = true,
            handsColorHex = "#FAF3E8",
            pendulumRodColorHex = "#E8A838",
            pendulumBobColorHex = "#E06B3E"
        ),
        VideoFacePalettePreset(
            key = "neon_rose",
            label = "Neon Rose",
            isDarkFace = true,
            handsColorHex = "#FF70A6",
            pendulumRodColorHex = "#70D6FF",
            pendulumBobColorHex = "#FFD670"
        ),
        VideoFacePalettePreset(
            key = "botanical_mint",
            label = "Botanical Mint",
            isDarkFace = true,
            handsColorHex = "#A8F0C6",
            pendulumRodColorHex = "#6FD4B8",
            pendulumBobColorHex = "#2E9F72"
        ),
        VideoFacePalettePreset(
            key = "ink_bright",
            label = "Ink (Bright Video)",
            isDarkFace = false,
            handsColorHex = "#14161A",
            pendulumRodColorHex = "#4A4E58",
            pendulumBobColorHex = "#D94E34"
        )
    )
}

data class CustomWatchFace(
    val id: String,
    val title: String,
    val videoFileName: String,
    val thumbnailFileName: String,
    val durationMs: Long,
    val clipStartMs: Long,
    val clipEndMs: Long,
    val isDarkFace: Boolean = true,
    val handsColorHex: String = "#28F7E6",
    val pendulumRodColorHex: String = "#28F7E6",
    val pendulumBobColorHex: String = "#9D00DF",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toArtMeta(): KlydoArtMeta {
        val clipSec = ((clipEndMs - clipStartMs).coerceAtLeast(1000L) / 1000f)
        val clipLabel = String.format(java.util.Locale.US, "%.1fs loop", clipSec)
        return KlydoArtMeta(
            id = id,
            title = title,
            subtitle = "Custom $clipLabel video watch face",
            studioCredit = "Custom · $clipLabel",
            category = KlydoArtCategory.CUSTOM,
            isDarkFace = isDarkFace,
            handsColorHex = handsColorHex,
            pendulumRodColorHex = pendulumRodColorHex,
            pendulumBobColorHex = pendulumBobColorHex
        )
    }
}

data class WatchFaceLibraryState(
    val customFaces: List<CustomWatchFace> = emptyList(),
    val deletedPrebuiltIds: Set<String> = emptySet()
) {
    fun availableArtMetas(): List<KlydoArtMeta> {
        val customMetas = customFaces.map { it.toArtMeta() }
        val prebuiltMetas = KlydoArtCatalog.all.filterNot { it.id in deletedPrebuiltIds }
        return customMetas + prebuiltMetas
    }

    fun findCustomFace(id: String): CustomWatchFace? =
        customFaces.firstOrNull { it.id == id }
}
