package com.vertigo.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.core.view.WindowCompat
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import com.vertigo.launcher.compose.FileHunterScreen
import androidx.compose.runtime.CompositionLocalProvider

class FileHunterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Create an ImageLoader that supports video frames for thumbnails
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()

        setContent {
            val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    Surface {
                        FileHunterScreen(
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }
}
