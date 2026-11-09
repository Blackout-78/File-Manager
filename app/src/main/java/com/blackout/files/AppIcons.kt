package com.blackout.files

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

data class IconOpt(
    val label: String,
    val alias: String,
    val fg: Int,
    val bg: Int,
    val defaultOn: Boolean
)

object AppIcons {
    val options = listOf(
        IconOpt("Classic", "com.blackout.files.IconVault", R.drawable.ic_foreground, 0xFF0B0D12.toInt(), true),
        IconOpt("Ring", "com.blackout.files.IconNight", R.drawable.ic_foreground_night, 0xFF0B0D12.toInt(), false),
        IconOpt("Solid", "com.blackout.files.IconOutline", R.drawable.ic_foreground_outline, 0xFF8B7CF6.toInt(), false)
    )

    private fun cn(ctx: Context, alias: String) = ComponentName(ctx.packageName, alias)

    fun current(ctx: Context): String {
        val pm = ctx.packageManager
        for (o in options) {
            val on = when (pm.getComponentEnabledSetting(cn(ctx, o.alias))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                else -> o.defaultOn
            }
            if (on) return o.alias
        }
        return options[0].alias
    }

    fun set(ctx: Context, alias: String) {
        val pm = ctx.packageManager
        pm.setComponentEnabledSetting(
            cn(ctx, alias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (o in options) {
            if (o.alias != alias) {
                pm.setComponentEnabledSetting(
                    cn(ctx, o.alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
