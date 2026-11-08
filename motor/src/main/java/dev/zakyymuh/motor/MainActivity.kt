package dev.zakyymuh.motor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import dev.zakyymuh.timebox.ui.theme.DailyTheme
import dev.zakyymuh.timebox.ui.theme.Fresh

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light, like papacti: dark bar icons on the white page.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Fresh.Page.toArgb(), Fresh.Page.toArgb()),
        )
        setContent {
            DailyTheme {
                MotorNav(onClose = ::finish)
            }
        }
    }
}
