package com.chaudharyjatin115.pixelia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chaudharyjatin115.pixelia.ui.GalleryApp
import com.chaudharyjatin115.pixelia.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full edge-to-edge support with transparent system bars
        enableEdgeToEdge()

        val viewUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) {
            intent.data ?: intent.clipData?.getItemAt(0)?.uri
        } else null
        val viewMimeType: String? = intent?.type

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GalleryApp(
                        initialViewUri = viewUri,
                        initialViewMimeType = viewMimeType
                    )
                }
            }
        }
    }
}
