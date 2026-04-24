package com.sanjay.omnidocviewer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.sanjay.omnidocviewer.ui.theme.OmniDocViewerTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.sanjay.omnidocviewer.ui.MainScreen
import com.sanjay.omnidocviewer.viewer.PdfViewer
import com.sanjay.omnidocviewer.viewer.ExcelViewer

class MainActivity : ComponentActivity() {

    private var selectedFileUri by mutableStateOf<Uri?>(null)

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedFileUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OmniDocViewerTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    MainScreen(
                        onOpenClick = { filePicker.launch("*/*") },
                        onCloseDocument = { selectedFileUri = null },
                        fileUri = selectedFileUri,
                        modifier = Modifier.padding(innerPadding)
                    )

                }
            }
        }
    }
}



