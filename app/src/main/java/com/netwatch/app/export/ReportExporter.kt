package com.netwatch.app.export

import java.io.File

interface ReportExporter {
    suspend fun exportFormattedReport(): File
    suspend fun exportRawCsv(): File
    suspend fun exportRawJson(): File
    suspend fun exportPdfReport(): File
}
