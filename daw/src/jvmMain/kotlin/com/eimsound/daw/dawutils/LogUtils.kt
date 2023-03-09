package com.eimsound.daw.dawutils

import com.eimsound.daw.LOGS_PATH
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import kotlin.io.path.absolutePathString

private val fileDateFormatter = SimpleDateFormat("yyyyMMdd")
private val logDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
@Suppress("unused")
class FileLogHandler : FileHandler("${LOGS_PATH.absolutePathString()}/${fileDateFormatter.format(Date())}-%g-%u.log", 0, 100) {
    init {
        formatter = LogFormatter()
    }
}

class LogFormatter : Formatter() {
    override fun format(record: LogRecord): String {
        return "[${logDateFormatter.format(Date(record.millis))}] [${record.level}]: [${Thread.currentThread().name}/${record.loggerName}] ${record.message}\n"
    }
}
