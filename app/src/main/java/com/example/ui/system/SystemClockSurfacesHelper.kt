package com.example.ui.system

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.wallpaper.KlydoWallpaperService

object SystemClockSurfacesHelper {

    fun isKlydoLiveWallpaperActive(context: Context): Boolean {
        return try {
            val info = WallpaperManager.getInstance(context).wallpaperInfo
            info?.packageName == context.packageName &&
                info.serviceName == KlydoWallpaperService::class.java.name
        } catch (_: Exception) {
            false
        }
    }

    fun isWallpaperSupported(context: Context): Boolean {
        return try {
            WallpaperManager.getInstance(context).isWallpaperSupported
        } catch (_: Exception) {
            true
        }
    }

    fun launchLiveWallpaperPicker(context: Context): Boolean {
        val component = ComponentName(context, KlydoWallpaperService::class.java)
        val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(directIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                val chooser = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openDreamSettings(context: Context): Boolean {
        val dreamIntent = Intent(Settings.ACTION_DREAM_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(dreamIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
