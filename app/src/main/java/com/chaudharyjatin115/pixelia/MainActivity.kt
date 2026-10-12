package com.chaudharyjatin115.pixelia

import android.content.ClipData
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

        val action = intent?.action
        val isPicker = action == Intent.ACTION_GET_CONTENT || action == Intent.ACTION_PICK
        val allowMultiple = intent?.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) == true

        val viewUri: Uri? = if (action == Intent.ACTION_VIEW) {
            intent.data ?: intent.clipData?.getItemAt(0)?.uri
        } else null
        val viewMimeType: String? = intent?.type

        val onMediaPicked: (List<Uri>) -> Unit = { uris ->
            if (uris.isNotEmpty()) {
                val resultIntent = Intent()
                if (uris.size == 1) {
                    resultIntent.data = uris.first()
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    val clipData = ClipData.newUri(contentResolver, "Selected Media", uris.first())
                    for (i in 1 until uris.size) {
                        clipData.addItem(ClipData.Item(uris[i]))
                    }
                    resultIntent.clipData = clipData
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }

        val onPickerCancel: () -> Unit = {
            setResult(RESULT_CANCELED)
            finish()
        }

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GalleryApp(
                        initialViewUri = viewUri,
                        initialViewMimeType = viewMimeType,
                        isPicker = isPicker,
                        allowMultiple = allowMultiple,
                        onMediaPicked = onMediaPicked,
                        onPickerCancel = onPickerCancel
                    )
                }
            }
        }
    }
}
