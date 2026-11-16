package com.example.ui.clock

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.CustomWatchFace
import com.example.ui.clock.faces.AstrolabeGuildFace
import com.example.ui.clock.faces.AstroPupFace
import com.example.ui.clock.faces.AuroraBorealisFace
import com.example.ui.clock.faces.BauhausKineticFace
import com.example.ui.clock.faces.BeepBoopBotFace
import com.example.ui.clock.faces.BotanicalPulseFace
import com.example.ui.clock.faces.CelestialObservatoryFace
import com.example.ui.clock.faces.ChryslerBrassFace
import com.example.ui.clock.faces.CloudDinoFace
import com.example.ui.clock.faces.CustomVideoLoopFace
import com.example.ui.clock.faces.EclipseHaloFace
import com.example.ui.clock.faces.FireflyGladeFace
import com.example.ui.clock.faces.GalaxyBobaFace
import com.example.ui.clock.faces.GothicRoseFace
import com.example.ui.clock.faces.HarmonicSilkFace
import com.example.ui.clock.faces.IsometricAtriumFace
import com.example.ui.clock.faces.KineticMonolithFace
import com.example.ui.clock.faces.KirigamiLaceFace
import com.example.ui.clock.faces.KoiSanctuaryFace
import com.example.ui.clock.faces.LotusYantraFace
import com.example.ui.clock.faces.MidnightGrooveFace
import com.example.ui.clock.faces.MinimalAtelierFace
import com.example.ui.clock.faces.MochiNekoFace
import com.example.ui.clock.faces.MoireVelvetFace
import com.example.ui.clock.faces.NordicPenduleFace
import com.example.ui.clock.faces.OrigamiSoarFace
import com.example.ui.clock.faces.PrismJellyfishFace
import com.example.ui.clock.faces.PrismKaleidoscopeFace
import com.example.ui.clock.faces.PrismRefractionFace
import com.example.ui.clock.faces.RetroTelemetryFace
import com.example.ui.clock.faces.SplashDuckieFace
import com.example.ui.clock.faces.SunburstHorizonFace
import com.example.ui.clock.faces.TidalNocturneFace
import com.example.ui.clock.faces.ZenRippleFace
import com.example.ui.theme.NightAmber

@Composable
fun KlydoArtCanvas(
    klydoId: String,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    isNightMode: Boolean,
    modifier: Modifier = Modifier,
    customVideoFace: CustomWatchFace? = null,
    customVideoUri: Uri? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
    ) {
        if (customVideoFace != null && customVideoUri != null) {
            CustomVideoLoopFace(
                videoUri = customVideoUri,
                clipStartMs = customVideoFace.clipStartMs,
                clipEndMs = customVideoFace.clipEndMs,
                durationMs = customVideoFace.durationMs
            )
        } else {
            when (klydoId) {
            // Abstract
            "bauhaus_orbit" -> BauhausKineticFace(phaseProvider = phaseProvider)
            "retro_grid" -> RetroTelemetryFace(phaseProvider = phaseProvider)
            "silk_lissajous" -> HarmonicSilkFace(phaseProvider = phaseProvider)
            "fluid_aurora" -> AuroraBorealisFace(phaseProvider = phaseProvider)
            "chromatic_prism" -> PrismRefractionFace(phaseProvider = phaseProvider)
            "kinetic_moire" -> MoireVelvetFace(phaseProvider = phaseProvider)

            // Nature
            "bioluminescent_jelly" -> PrismJellyfishFace(phaseProvider = phaseProvider)
            "sunburst_horizon" -> SunburstHorizonFace(
                phaseProvider = phaseProvider,
                minuteOfDayProvider = minuteOfDayProvider
            )
            "botanical_pulse" -> BotanicalPulseFace(phaseProvider = phaseProvider)
            "koi_lotus_pond" -> KoiSanctuaryFace(phaseProvider = phaseProvider)
            "firefly_forest" -> FireflyGladeFace(phaseProvider = phaseProvider)
            "tidal_nocturne" -> TidalNocturneFace(phaseProvider = phaseProvider)

            // Architecture
            "celestial_observatory" -> CelestialObservatoryFace(phaseProvider = phaseProvider)
            "kinetic_monolith" -> KineticMonolithFace(phaseProvider = phaseProvider)
            "rose_cathedral" -> GothicRoseFace(phaseProvider = phaseProvider)
            "art_deco_spire" -> ChryslerBrassFace(phaseProvider = phaseProvider)
            "escher_atrium" -> IsometricAtriumFace(phaseProvider = phaseProvider)

            // Mandala
            "sacred_lotus_mandala" -> LotusYantraFace(phaseProvider = phaseProvider)
            "kaleidoscope_bloom" -> PrismKaleidoscopeFace(phaseProvider = phaseProvider)
            "celestial_astrolabe" -> AstrolabeGuildFace(phaseProvider = phaseProvider)
            "paper_cut_mandala" -> KirigamiLaceFace(phaseProvider = phaseProvider)

            // Cartoons
            "sleepy_neko" -> MochiNekoFace(phaseProvider = phaseProvider)
            "astro_pup" -> AstroPupFace(phaseProvider = phaseProvider)
            "retro_bot" -> BeepBoopBotFace(phaseProvider = phaseProvider)
            "tiny_dino_cloud" -> CloudDinoFace(phaseProvider = phaseProvider)

            // Fun
            "rubber_duck_float" -> SplashDuckieFace(phaseProvider = phaseProvider)
            "disco_vinyl" -> MidnightGrooveFace(phaseProvider = phaseProvider)
            "cosmic_boba" -> GalaxyBobaFace(phaseProvider = phaseProvider)
            "paper_plane_sky" -> OrigamiSoarFace(phaseProvider = phaseProvider)

            // Minimal
            "minimal_atelier" -> MinimalAtelierFace(phaseProvider = phaseProvider)
            "zen_ripple" -> ZenRippleFace(phaseProvider = phaseProvider)
            "nordic_pendule" -> NordicPenduleFace(phaseProvider = phaseProvider)
            "eclipse_halo" -> EclipseHaloFace(phaseProvider = phaseProvider)

            else -> PrismJellyfishFace(phaseProvider = phaseProvider)
            }
        }

        if (isNightMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color(0x88000000))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NightAmber.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.5f
                    )
                )
            }
        }
    }
}
