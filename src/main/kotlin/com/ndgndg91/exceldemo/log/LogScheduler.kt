package com.ndgndg91.exceldemo.log

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(name = ["scheduler.enabled"], havingValue = "true", matchIfMissing = false)
class LogScheduler(
    private val mockLogGenerator: MockLogGenerator,
    private val logProducer: LogProducer,
    @Value("\${scheduler.max-records:1000000}") private val maxRecords: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val totalGenerated = AtomicLong(0)

    companion object {
        private const val BATCH_SIZE = 500
    }

    @Scheduled(fixedRate = 500) // Run every 500 m second
    fun generateAndSendLogs() {
        val currentCount = totalGenerated.get()
        if (currentCount >= maxRecords) {
            if (currentCount == maxRecords) { // Log this message only once
                log.info("Reached the maximum number of records to generate ({}). Stopping scheduler.", maxRecords)
                totalGenerated.incrementAndGet() // Increment once more to prevent re-logging
            }
            return
        }

        log.info("Generating and sending a batch of {} logs... (Total generated: {})", BATCH_SIZE, currentCount)
        val logs = (1..BATCH_SIZE).map { mockLogGenerator.generate() }
        logProducer.sendBatch(logs).thenRun {
            totalGenerated.addAndGet(BATCH_SIZE.toLong())
        }
    }
}