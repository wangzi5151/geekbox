package com.tinyai.geekbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tinyai.geekbox.core.settings.AppSettings
import com.tinyai.geekbox.core.settings.ThemeMode
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.ui.theme.GeekBoxTheme
import com.tinyai.geekbox.navigation.GeekBoxApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        ShizukuBridge.init()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = when (AppSettings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            GeekBoxTheme(darkTheme = dark, dynamicColor = AppSettings.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GeekBoxApp()
                }
            }
        }
    }
}
