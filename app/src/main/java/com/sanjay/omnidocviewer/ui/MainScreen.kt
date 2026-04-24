package com.sanjay.omnidocviewer.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjay.omnidocviewer.viewer.PdfViewer
import com.sanjay.omnidocviewer.viewer.ExcelViewer
import com.sanjay.omnidocviewer.viewer.TextViewer
import com.sanjay.omnidocviewer.utils.PptToPdfConverter

@Composable
fun MainScreen(
    onOpenClick: () -> Unit,
    onCloseDocument: () -> Unit,
    fileUri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // =========================
    // FILE ROUTING
    // =========================
    if (fileUri != null) {

        val fileName = remember(fileUri) {
            var name: String? = null
            context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    name = cursor.getString(index)
                }
            }
            name?.lowercase() ?: ""
        }

        // ---------- PDF ----------
        if (fileName.endsWith(".pdf")) {
            BackHandler { onCloseDocument() }
            PdfViewer(uri = fileUri, onClose = onCloseDocument)
            return
        }

        // ---------- TEXT ----------
        if (fileName.endsWith(".txt") || fileName.endsWith(".xml")) {
            BackHandler { onCloseDocument() }
            TextViewer(uri = fileUri, onClose = onCloseDocument)
            return
        }

        // ---------- EXCEL ----------
        if (fileName.endsWith(".xlsx")) {
            BackHandler { onCloseDocument() }
            ExcelViewer(uri = fileUri, onClose = onCloseDocument)
            return
        }

        // ---------- PPT / PPTX ----------
        if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {

            var isConverting by remember { mutableStateOf(true) }
            var pdfUri by remember { mutableStateOf<Uri?>(null) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(fileUri) {
                try {
                    val file = PptToPdfConverter.convert(context.contentResolver, fileUri)
                    pdfUri = Uri.fromFile(file)
                } catch (e: Exception) {
                    error = e.message ?: "Conversion failed"
                }
                isConverting = false
            }

            when {
                isConverting -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Converting PPT → PDF...")
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("❌ $error")
                    }
                }

                pdfUri != null -> {
                    PdfViewer(uri = pdfUri!!, onClose = onCloseDocument)
                }
            }

            return
        }
    }

    // =========================
    // HOME UI (ALWAYS VISIBLE WHEN NO FILE)
    // =========================
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "OmniDoc Viewer",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = onOpenClick) {
            Text("Open Document")
        }
    }
}