package com.sanjay.omnidocviewer.viewer

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.github.barteksc.pdfviewer.PDFView
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(uri: Uri, onClose: () -> Unit) {

    val context = LocalContext.current

    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(1) }
    var fileName by remember { mutableStateOf("Document") }

    LaunchedEffect(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text(fileName) },
            navigationIcon = {
                TextButton(onClick = onClose) {
                    Text("Back")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {

            AndroidView(
                factory = { ctx ->
                    PDFView(ctx, null).apply {

                        val inputStream = when (uri.scheme) {
                            "content" -> context.contentResolver.openInputStream(uri)
                            "file", null -> java.io.FileInputStream(java.io.File(uri.path!!))
                            else -> java.net.URL(uri.toString()).openStream()
                        }

                        fromStream(inputStream)
                            .enableSwipe(true)
                            .enableDoubletap(true)
                            .swipeHorizontal(false)
                            .onPageChange { page, pageCountTotal ->
                                currentPage = page + 1
                                pageCount = pageCountTotal
                            }
                            .load()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .padding(horizontal = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Page $currentPage / $pageCount",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}