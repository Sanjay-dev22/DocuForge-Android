package com.sanjay.omnidocviewer.viewer

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sanjay.omnidocviewer.utils.FileIndexEngine
import kotlinx.coroutines.launch
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewer(uri: Uri, onClose: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val engine = remember { FileIndexEngine(context.contentResolver, uri) }

    var fileName by remember { mutableStateOf("Text File") }

    var reader by remember { mutableStateOf<BufferedReader?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }

    var totalLines by remember { mutableStateOf(0) }

    // 🔍 SEARCH
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(0) }

    // 🔢 JUMP
    var jumpLineInput by remember { mutableStateOf("") }
    var highlightedLine by remember { mutableStateOf(-1) }

    var isIndexing by remember { mutableStateOf(true) }

    // =========================
    // LOAD MORE
    // =========================
    suspend fun loadMoreLines() {
        if (isLoading || endReached) return
        isLoading = true

        val newLines = mutableListOf<String>()
        val r = reader ?: return

        repeat(200) {
            val line = r.readLine()
            if (line == null) {
                endReached = true
                return@repeat
            }
            newLines.add(line)
        }

        lines = lines + newLines
        isLoading = false
    }

    suspend fun ensureLineLoaded(target: Int) {
        while (lines.size <= target && !endReached) {
            loadMoreLines()
        }
    }

    // =========================
    // INIT
    // =========================
    LaunchedEffect(uri) {

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(index)
            }
        }

        scope.launch {
            totalLines = engine.countLines()
        }

        // Build index
        isIndexing = true
        isIndexing = false

        reader = context.contentResolver.openInputStream(uri)?.bufferedReader()
        loadMoreLines()
    }

    // =========================
    // SEARCH LOGIC (FULL FILE)
    // =========================
    fun performSearch(query: String) {

        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        scope.launch {

            val results = engine.search(query)

            searchResults = results
            currentMatchIndex = 0

            if (results.isNotEmpty()) {
                val first = results[0]
                ensureLineLoaded(first)
                listState.scrollToItem(first)
                highlightedLine = first
            }
        }
    }

    fun goToMatch(index: Int) {
        if (searchResults.isEmpty()) return

        val safeIndex = index.coerceIn(0, searchResults.size - 1)
        currentMatchIndex = safeIndex

        scope.launch {
            val line = searchResults[safeIndex]
            ensureLineLoaded(line)
            listState.scrollToItem(line)
            highlightedLine = line
        }
    }

    // =========================
    // INFINITE SCROLL
    // =========================
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= lines.size - 20
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMoreLines()
    }

    BackHandler { onClose() }

    // =========================
    // UI (PRO LEVEL)
    // =========================
    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(fileName)
                    Text(
                        text = "$totalLines lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            navigationIcon = {
                TextButton(onClick = onClose) { Text("Back") }
            }
        )

        // 🔍 SEARCH BAR (PRIMARY)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                performSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search in file...") },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        // 🔢 SECONDARY ACTION ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            OutlinedTextField(
                value = jumpLineInput,
                onValueChange = { jumpLineInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Go to line") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    val line = jumpLineInput.toIntOrNull()?.minus(1)
                    if (line != null && line >= 0) {
                        scope.launch {
                            ensureLineLoaded(line)
                            listState.scrollToItem(line)
                            highlightedLine = line
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Go")
            }
        }

        // 🔍 SEARCH INFO + NAVIGATION
        if (searchResults.isNotEmpty()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text("${searchResults.size} matches")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    TextButton(onClick = {
                        if (searchResults.isNotEmpty()) {
                            currentMatchIndex =
                                (currentMatchIndex - 1 + searchResults.size) % searchResults.size

                            val line = searchResults[currentMatchIndex]

                            scope.launch {
                                ensureLineLoaded(line)
                                listState.scrollToItem(line)
                                highlightedLine = line
                            }
                        }
                    }) {
                        Text("Prev")
                    }

                    TextButton(onClick = {
                        if (searchResults.isNotEmpty()) {
                            currentMatchIndex =
                                (currentMatchIndex + 1) % searchResults.size

                            val line = searchResults[currentMatchIndex]

                            scope.launch {
                                ensureLineLoaded(line)
                                listState.scrollToItem(line)
                                highlightedLine = line
                            }
                        }
                    }) {
                        Text("Next")
                    }
                }
            }
        }

        Divider()

        // 📄 CONTENT
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {

            itemsIndexed(lines) { index, line ->

                val isHighlighted = highlightedLine == index

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isHighlighted -> Color(0xFF2E7D32).copy(alpha = 0.4f)
                                index % 2 == 0 -> Color.Transparent
                                else -> Color(0xFF000000).copy(alpha = 0.03f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {

                    // Line number
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.width(60.dp),
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )

                    // Content
                    Text(
                        text = line,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
