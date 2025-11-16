package com.ndgndg91.exceldemo.log

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LogController(private val logProducer: LogProducer) {

    @PostMapping("/log")
    fun sendSingleLog(): ResponseEntity<String> {
        val sampleLog = UserHistoryLog(
            accountId = 12345L,
            type = "LOGIN",
            contextData = mapOf(
                "ip_address" to "192.168.1.100",
                "device" to "web"
            )
        )
        logProducer.send(sampleLog)
        return ResponseEntity.ok("Sent a single log to Firehose.")
    }

    @PostMapping("/log-batch")
    fun sendBatchLogs(): ResponseEntity<String> {
        val sampleLogs = listOf(
            UserHistoryLog(
                accountId = 67890L,
                type = "PURCHASE",
                contextData = mapOf(
                    "item_id" to "item-001",
                    "price" to "15000"
                )
            ),
            UserHistoryLog(
                accountId = 54321L,
                type = "CERT_ISSUE",
                contextData = mapOf(
                    "cert_type" to "SIGN_UP",
                    "issuer" to "government"
                )
            )
        )
        logProducer.sendBatch(sampleLogs)
        return ResponseEntity.ok("Sent a batch of logs to Firehose.")
    }
}
