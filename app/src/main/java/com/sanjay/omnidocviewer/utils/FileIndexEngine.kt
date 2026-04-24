package com.sanjay.omnidocviewer.utils

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FileIndexEngine(
    private val resolver: ContentResolver,
    private val uri: Uri
) {

    suspend fun search(query: String): List<Int> = withContext(Dispatchers.IO) {

        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<Int>()

        resolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->

                var lineNumber = 0

                reader.forEachLine { line ->
                    if (line.contains(query, ignoreCase = true)) {
                        results.add(lineNumber)
                    }
                    lineNumber++
                }
            }
        }

        results
    }

    suspend fun countLines(): Int = withContext(Dispatchers.IO) {

        var count = 0

        resolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                while (reader.readLine() != null) {
                    count++
                }
            }
        }

        count
    }
}