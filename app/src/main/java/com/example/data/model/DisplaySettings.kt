package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PendulumStyle(val key: String, val label: String) {
    OBSIDIAN("obsidian", "Sync with Face"),
    BRASS("brass", "Brushed Brass"),
    TERRACOTTA("terracotta", "Terracotta Lacquer"),
    MINIMAL("minimal", "Minimal Ring");

    companion object {
        fun fromKey(key: String): PendulumStyle =
            entries.firstOrNull { it.key == key } ?: OBSIDIAN
    }
}

enum class KlydoArtCategory(val label: String) {
    CUSTOM("Custom"),
    ABSTRACT("Abstract"),
    NATURE("Nature"),
    ARCHITECTURE("Architecture"),
    MANDALA("Mandala"),
    CARTOONS("Cartoons"),
    FUN("Fun"),
    MINIMAL("Minimal")
}

data class KlydoArtMeta(
    val id: String,
    val title: String,
    val subtitle: String,
    val studioCredit: String,
    val category: KlydoArtCategory,
    val isDarkFace: Boolean,
    val handsColorHex: String,
    val pendulumRodColorHex: String,
    val pendulumBobColorHex: String
)

object KlydoArtCatalog {
    val all: List<KlydoArtMeta> = listOf(
        // --- ABSTRACT ---
        KlydoArtMeta(
            id = "bauhaus_orbit",
            title = "Bauhaus Kinetic",
            subtitle = "Crisp geometric arcs & orbiting spheres in terracotta & saffron",
            studioCredit = "Dessau Form Series",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#F5EFE4",
            pendulumRodColorHex = "#E8A838",
            pendulumBobColorHex = "#E06B3E"
        ),
        KlydoArtMeta(
            id = "retro_grid",
            title = "Iso Telemetry",
            subtitle = "Precision vector grid with rhythmic neon horizon",
            studioCredit = "Module 08",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#FF47B8",
            pendulumRodColorHex = "#28F7E6",
            pendulumBobColorHex = "#E024A8"
        ),
        KlydoArtMeta(
            id = "silk_lissajous",
            title = "Harmonic Silk",
            subtitle = "Precessing Lissajous platinum filament & luminous pearl node",
            studioCredit = "Atelier Lissajous",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#F4EDE2",
            pendulumRodColorHex = "#D6C3A5",
            pendulumBobColorHex = "#E2A898"
        ),
        KlydoArtMeta(
            id = "fluid_aurora",
            title = "Aurora Borealis",
            subtitle = "Luminous polar curtains shimmering above sub-arctic peaks",
            studioCredit = "Nordic Ether",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#A4FFEE",
            pendulumRodColorHex = "#70E1F5",
            pendulumBobColorHex = "#9D6BFF"
        ),
        KlydoArtMeta(
            id = "chromatic_prism",
            title = "Prism Refraction",
            subtitle = "Spinning crystal prism splitting white light into spectral beams",
            studioCredit = "Optic Atelier",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#FFFFFF",
            pendulumRodColorHex = "#E2E8F0",
            pendulumBobColorHex = "#FF5EA8"
        ),
        KlydoArtMeta(
            id = "kinetic_moire",
            title = "Moiré Velvet",
            subtitle = "Counter-oscillating fine lattices creating hypnotic optical waves",
            studioCredit = "Optic Kinetic",
            category = KlydoArtCategory.ABSTRACT,
            isDarkFace = true,
            handsColorHex = "#FFE39F",
            pendulumRodColorHex = "#9BB7FF",
            pendulumBobColorHex = "#FF7E6B"
        ),

        // --- NATURE ---
        KlydoArtMeta(
            id = "bioluminescent_jelly",
            title = "Prism Jellyfish",
            subtitle = "Bioluminescent violet, magenta & cyan drifter on pure black",
            studioCredit = "Klydo Signature",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#28F7E6",
            pendulumRodColorHex = "#28F7E6",
            pendulumBobColorHex = "#9D00DF"
        ),
        KlydoArtMeta(
            id = "sunburst_horizon",
            title = "Solar Horizon",
            subtitle = "Dynamic daylight gradient shifting from dawn to dusk",
            studioCredit = "Chrono Light Lab",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#FFF3D6",
            pendulumRodColorHex = "#FF7B54",
            pendulumBobColorHex = "#FFB238"
        ),
        KlydoArtMeta(
            id = "botanical_pulse",
            title = "Botanical Pulse",
            subtitle = "Kinetic sage & ochre petals breathing with the pendulum",
            studioCredit = "Kyoto Atelier",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#A8F0C6",
            pendulumRodColorHex = "#A8F0C6",
            pendulumBobColorHex = "#2E9F72"
        ),
        KlydoArtMeta(
            id = "koi_lotus_pond",
            title = "Koi Sanctuary",
            subtitle = "Articulated Kohaku & Yamabuki koi gliding beneath floating lotus pads",
            studioCredit = "Kyoto Watergarden",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#FFF3E2",
            pendulumRodColorHex = "#6FD4B8",
            pendulumBobColorHex = "#FF653D"
        ),
        KlydoArtMeta(
            id = "firefly_forest",
            title = "Firefly Glade",
            subtitle = "Warm bioluminescent fireflies drifting through a moonlit pine canopy",
            studioCredit = "Twilight Canopy",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#FFE89E",
            pendulumRodColorHex = "#8EC5B6",
            pendulumBobColorHex = "#FFD15C"
        ),
        KlydoArtMeta(
            id = "tidal_nocturne",
            title = "Tidal Nocturne",
            subtitle = "Moonlit ocean swells crested with glowing bioluminescent foam",
            studioCredit = "Pelagic Moon",
            category = KlydoArtCategory.NATURE,
            isDarkFace = true,
            handsColorHex = "#E6F4FA",
            pendulumRodColorHex = "#78C2D4",
            pendulumBobColorHex = "#F2D59B"
        ),

        // --- ARCHITECTURE ---
        KlydoArtMeta(
            id = "celestial_observatory",
            title = "Celestial Observatory",
            subtitle = "Midnight indigo orbital rings & drifting stars",
            studioCredit = "Solstice Edition",
            category = KlydoArtCategory.ARCHITECTURE,
            isDarkFace = true,
            handsColorHex = "#F5C451",
            pendulumRodColorHex = "#F5C451",
            pendulumBobColorHex = "#3E6BFF"
        ),
        KlydoArtMeta(
            id = "kinetic_monolith",
            title = "Calder Balance",
            subtitle = "Golden-ratio suspended mobile counterweights over architectural arcs",
            studioCredit = "Monolith Form",
            category = KlydoArtCategory.ARCHITECTURE,
            isDarkFace = true,
            handsColorHex = "#F2E9DC",
            pendulumRodColorHex = "#8FAEC4",
            pendulumBobColorHex = "#D98A4E"
        ),
        KlydoArtMeta(
            id = "rose_cathedral",
            title = "Gothic Rose",
            subtitle = "Stained-glass rose tracery illuminated by drifting golden sunbeams",
            studioCredit = "Chartres Light",
            category = KlydoArtCategory.ARCHITECTURE,
            isDarkFace = true,
            handsColorHex = "#FFF1D0",
            pendulumRodColorHex = "#E8B84E",
            pendulumBobColorHex = "#D93B58"
        ),
        KlydoArtMeta(
            id = "art_deco_spire",
            title = "Chrysler Brass",
            subtitle = "Gilded 1930s sunburst arches & sweeping obsidian searchlights",
            studioCredit = "Manhattan Deco",
            category = KlydoArtCategory.ARCHITECTURE,
            isDarkFace = true,
            handsColorHex = "#FBE3A4",
            pendulumRodColorHex = "#D4A853",
            pendulumBobColorHex = "#45B8AC"
        ),
        KlydoArtMeta(
            id = "escher_atrium",
            title = "Isometric Atrium",
            subtitle = "Breathing sunlit limestone monoliths in precision isometric projection",
            studioCredit = "Arch Studio",
            category = KlydoArtCategory.ARCHITECTURE,
            isDarkFace = true,
            handsColorHex = "#FAF3E8",
            pendulumRodColorHex = "#7CA1BD",
            pendulumBobColorHex = "#E57A54"
        ),

        // --- MANDALA ---
        KlydoArtMeta(
            id = "sacred_lotus_mandala",
            title = "Lotus Yantra",
            subtitle = "Ornate counter-rotating lotus filigree & golden sacred geometry",
            studioCredit = "Vedic Geometry",
            category = KlydoArtCategory.MANDALA,
            isDarkFace = true,
            handsColorHex = "#FFE49E",
            pendulumRodColorHex = "#E5A93C",
            pendulumBobColorHex = "#E85D75"
        ),
        KlydoArtMeta(
            id = "kaleidoscope_bloom",
            title = "Prism Kaleidoscope",
            subtitle = "12-fold mirrored crystal shards blooming in teal, coral & gold",
            studioCredit = "Mirror Atelier",
            category = KlydoArtCategory.MANDALA,
            isDarkFace = true,
            handsColorHex = "#FFFFFF",
            pendulumRodColorHex = "#5EEAD4",
            pendulumBobColorHex = "#F472B6"
        ),
        KlydoArtMeta(
            id = "celestial_astrolabe",
            title = "Astrolabe Guild",
            subtitle = "Engraved antique brass rete, lapis dial & orbiting lunar phase orb",
            studioCredit = "Almagest Brass",
            category = KlydoArtCategory.MANDALA,
            isDarkFace = true,
            handsColorHex = "#F7DC98",
            pendulumRodColorHex = "#C99D44",
            pendulumBobColorHex = "#3D7EFF"
        ),
        KlydoArtMeta(
            id = "paper_cut_mandala",
            title = "Kirigami Lace",
            subtitle = "Layered cut-paper washi rosettes casting gentle architectural shadows",
            studioCredit = "Washi Studio",
            category = KlydoArtCategory.MANDALA,
            isDarkFace = false,
            handsColorHex = "#1B2028",
            pendulumRodColorHex = "#54687A",
            pendulumBobColorHex = "#D64933"
        ),

        // --- CARTOONS ---
        KlydoArtMeta(
            id = "sleepy_neko",
            title = "Mochi Neko",
            subtitle = "Playful calico kitty with twitching ears, blinking eyes & swinging tail",
            studioCredit = "Tokyo Kawaii",
            category = KlydoArtCategory.CARTOONS,
            isDarkFace = true,
            handsColorHex = "#FF8FAB",
            pendulumRodColorHex = "#FFD1DC",
            pendulumBobColorHex = "#FF6B9D"
        ),
        KlydoArtMeta(
            id = "astro_pup",
            title = "Astro Pup",
            subtitle = "Space-helmet Shiba pup floating happily beside an orbiting bone",
            studioCredit = "Cosmic Toons",
            category = KlydoArtCategory.CARTOONS,
            isDarkFace = true,
            handsColorHex = "#5CE1E6",
            pendulumRodColorHex = "#FFD166",
            pendulumBobColorHex = "#FF7A59"
        ),
        KlydoArtMeta(
            id = "retro_bot",
            title = "Beep-Boop Bot",
            subtitle = "Expressive retro robot with glowing visor eyes & bouncing spring antenna",
            studioCredit = "Pixel & Ink",
            category = KlydoArtCategory.CARTOONS,
            isDarkFace = true,
            handsColorHex = "#52FFB8",
            pendulumRodColorHex = "#60A5FA",
            pendulumBobColorHex = "#FF5964"
        ),
        KlydoArtMeta(
            id = "tiny_dino_cloud",
            title = "Cloud Dino",
            subtitle = "Cheerful pastel dino bouncing beneath sunlit storybook clouds",
            studioCredit = "Storybook Pop",
            category = KlydoArtCategory.CARTOONS,
            isDarkFace = false,
            handsColorHex = "#1F2937",
            pendulumRodColorHex = "#34D399",
            pendulumBobColorHex = "#FB7185"
        ),

        // --- FUN ---
        KlydoArtMeta(
            id = "rubber_duck_float",
            title = "Splash Duckie",
            subtitle = "Sunny yellow ducky bobbing on turquoise waves with iridescent bubbles",
            studioCredit = "Bathhouse Pop",
            category = KlydoArtCategory.FUN,
            isDarkFace = true,
            handsColorHex = "#FFFFFF",
            pendulumRodColorHex = "#FFE066",
            pendulumBobColorHex = "#FF6B35"
        ),
        KlydoArtMeta(
            id = "disco_vinyl",
            title = "Midnight Groove",
            subtitle = "Spinning iridescent LP record with tracking tonearm & pulse bars",
            studioCredit = "Vinyl Club",
            category = KlydoArtCategory.FUN,
            isDarkFace = true,
            handsColorHex = "#FF70A6",
            pendulumRodColorHex = "#70D6FF",
            pendulumBobColorHex = "#FFD670"
        ),
        KlydoArtMeta(
            id = "cosmic_boba",
            title = "Galaxy Boba",
            subtitle = "Smiling taro milk-tea cup with bouncy pearls & pastel star sprinkles",
            studioCredit = "Playful Pop",
            category = KlydoArtCategory.FUN,
            isDarkFace = true,
            handsColorHex = "#FFF0F5",
            pendulumRodColorHex = "#C4B5FD",
            pendulumBobColorHex = "#F472B6"
        ),
        KlydoArtMeta(
            id = "paper_plane_sky",
            title = "Origami Soar",
            subtitle = "Crisp crimson paper plane gliding figure-8 loops across sunny clouds",
            studioCredit = "Skyline Play",
            category = KlydoArtCategory.FUN,
            isDarkFace = false,
            handsColorHex = "#1E293B",
            pendulumRodColorHex = "#3B82F6",
            pendulumBobColorHex = "#EF4444"
        ),

        // --- MINIMAL ---
        KlydoArtMeta(
            id = "minimal_atelier",
            title = "Rams Dial",
            subtitle = "High-contrast warm cream reference face for room clarity",
            studioCredit = "Functional Studio",
            category = KlydoArtCategory.MINIMAL,
            isDarkFace = false,
            handsColorHex = "#14161A",
            pendulumRodColorHex = "#343842",
            pendulumBobColorHex = "#E06B3E"
        ),
        KlydoArtMeta(
            id = "zen_ripple",
            title = "Karesansui Ripple",
            subtitle = "Meditative sumi-ink acoustic ripples & celadon crescent",
            studioCredit = "Kyoto Minimal",
            category = KlydoArtCategory.MINIMAL,
            isDarkFace = true,
            handsColorHex = "#EAE6DF",
            pendulumRodColorHex = "#8FA89B",
            pendulumBobColorHex = "#5D8A7B"
        ),
        KlydoArtMeta(
            id = "nordic_pendule",
            title = "Pendule Équilibre",
            subtitle = "Warm alabaster dial with balanced graphite arc & vermilion jewel",
            studioCredit = "Geneva Studio",
            category = KlydoArtCategory.MINIMAL,
            isDarkFace = false,
            handsColorHex = "#181A1F",
            pendulumRodColorHex = "#4A4E58",
            pendulumBobColorHex = "#D94E34"
        ),
        KlydoArtMeta(
            id = "eclipse_halo",
            title = "Penumbra",
            subtitle = "Chiaroscuro solar corona drifting around a pure obsidian umbra",
            studioCredit = "Nocturne Mono",
            category = KlydoArtCategory.MINIMAL,
            isDarkFace = true,
            handsColorHex = "#F7F3EA",
            pendulumRodColorHex = "#B8C0CC",
            pendulumBobColorHex = "#E5B869"
        )
    )

    fun findById(id: String): KlydoArtMeta =
        all.firstOrNull { it.id == id } ?: all.first()
}

@IgnoreExtraProperties
data class DisplaySettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activeKlydoId: String = "bioluminescent_jelly",
    val autoCycleArt: Boolean = false,
    val cycleIntervalMinutes: Long = 60L,
    val pendulumStyle: String = PendulumStyle.OBSIDIAN.key,
    val pendulumEnabled: Boolean = true,
    val hourlyChimeEnabled: Boolean = true,
    val blockChimeEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val nightModeEnabled: Boolean = false,
    val nightStartMinutes: Long = 1320L,
    val nightEndMinutes: Long = 420L,
    val clockFormat24h: Boolean = false,
    val showFrame: Boolean = true,
    val favoriteKlydoIds: List<String> = listOf("bioluminescent_jelly"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
