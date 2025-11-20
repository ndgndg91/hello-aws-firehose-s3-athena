package com.ndgndg91.exceldemo.log

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(name = ["scheduler.enabled"], havingValue = "true", matchIfMissing = false)
class LogScheduler(
    private val mockLogGenerator: MockLogGenerator,
    private val logProducer: LogProducer,
    @param:Value("\${scheduler.max-records:1000000}") private val maxRecords: Long,
    @param:Value("\${mock.log.generator.date-mode:today}") private val dateMode: String,
    @param:Value("\${mock.log.generator.specific-date:}") private val specificDateStr: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val totalGenerated = AtomicLong(0)
    private val startEpochDay = LocalDate.of(2025, 1, 1).toEpochDay()


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

        // Determine the date for this batch once
        val targetDate = when (dateMode.lowercase()) {
            "random" -> {
                val todayEpochDay = LocalDate.now().toEpochDay()
                val randomEpochDay = ThreadLocalRandom.current().nextLong(startEpochDay, todayEpochDay + 1)
                LocalDate.ofEpochDay(randomEpochDay)
            }
            "specific" -> {
                try {
                    LocalDate.parse(specificDateStr)
                } catch (e: DateTimeParseException) {
                    log.warn("Invalid specific-date '{}'. Defaulting to today.", specificDateStr, e)
                    LocalDate.now()
                }
            }
            "today" -> LocalDate.now()
            else -> {
                log.warn("Unknown date-mode '{}'. Defaulting to today.", dateMode)
                LocalDate.now()
            }
        }

        log.info("Generating and sending a batch of {} logs for date {}... (Total generated: {})", BATCH_SIZE, targetDate, currentCount)
        val logs = (1..BATCH_SIZE).map { mockLogGenerator.generate(targetDate) }
        logProducer.sendBatch(logs).thenRun {
            totalGenerated.addAndGet(BATCH_SIZE.toLong())
        }
    }
}