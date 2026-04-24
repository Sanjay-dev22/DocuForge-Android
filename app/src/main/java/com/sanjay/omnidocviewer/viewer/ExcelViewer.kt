package com.sanjay.omnidocviewer.viewer

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelViewer(uri: Uri, onClose: () -> Unit) {

    val context = LocalContext.current

    var sheetData by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var columnWidths by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(uri) {

        try {

            val inputStream = context.contentResolver.openInputStream(uri)
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            val rows = mutableListOf<List<String>>()
            val colWidthMap = mutableMapOf<Int, Int>()

            for (row in sheet) {

                val cells = mutableListOf<String>()

                for (cell in row) {

                    val text = cell.toString()
                    val colIndex = cell.columnIndex

                    val currentMax = colWidthMap[colIndex] ?: 0
                    colWidthMap[colIndex] = maxOf(currentMax, text.length)

                    cells.add(text)
                }

                rows.add(cells)
            }

            sheetData = rows
            columnWidths = colWidthMap.values.toList()

        } catch (e: Exception) {
            sheetData = listOf(listOf("Failed to open Excel file"))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Excel Viewer") },
            navigationIcon = {
                TextButton(onClick = onClose) {
                    Text("Back")
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {

            Column {

                sheetData.forEach { row ->

                    Row {

                        row.forEachIndexed { index, cell ->

                            val width = if (index < columnWidths.size)
                                columnWidths[index] * 12
                            else
                                120

                            Box(
                                modifier = Modifier
                                    .width(width.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(8.dp)
                            ) {

                                Text(cell)

                            }

                        }

                    }

                }

            }

        }
    }
}