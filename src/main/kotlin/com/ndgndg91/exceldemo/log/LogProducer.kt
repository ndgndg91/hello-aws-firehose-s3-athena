package com.ndgndg91.exceldemo.log

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.PutRecordRequest
import software.amazon.awssdk.services.firehose.model.Record
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@Service
class LogProducer(
    private val firehoseClient: FirehoseAsyncClient,
    private val objectMapper: ObjectMapper,
    private val mockLogGenerator: MockLogGenerator,
    @param:Value("\${aws.firehose.stream-name}") private val streamName: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun produce(targetDate: LocalDate) {
        val userHistoryLog = mockLogGenerator.generate(targetDate)
        send(userHistoryLog)
    }

    fun produce() {
        produce(LocalDate.now())
    }

    fun send(log: UserHistoryLog): CompletableFuture<Void> {
        val jsonLog = objectMapper.writeValueAsString(log)
        val record = Record.builder()
            .data(SdkBytes.fromUtf8String(jsonLog))
            .build()

        val request = PutRecordRequest.builder()
            .deliveryStreamName(streamName)
            .record(record)
            .build()

        return firehoseClient.putRecord(request)
            .thenAccept { response ->
                this.log.info("Successfully sent a record to Firehose. RecordId: {}", response.recordId())
            }.exceptionally { e ->
                this.log.error("Failed to send a record to Firehose.", e)
                null
            }
    }

    fun sendBatch(logs: List<UserHistoryLog>): CompletableFuture<Void> {
        if (logs.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        val records = logs.map { userHistoryLog ->
            val jsonLog = objectMapper.writeValueAsString(userHistoryLog)
            Record.builder()
                .data(SdkBytes.fromUtf8String(jsonLog))
                .build()
        }

        val request = PutRecordBatchRequest.builder()
            .deliveryStreamName(streamName)
            .records(records)
            .build()

        return firehoseClient.putRecordBatch(request)
            .thenAccept { response ->
                val failedCount = response.failedPutCount()
                if (failedCount > 0) {
                    this.log.warn("Sent a batch of {} records, but {} failed.", logs.size, failedCount)
                    response.requestResponses().forEachIndexed { index, putRecordBatchResponseEntry ->
                        if (putRecordBatchResponseEntry.errorCode() != null) {
                            this.log.warn(
                                "Record #{} failed with code: {}, message: {}",
                                index,
                                putRecordBatchResponseEntry.errorCode(),
                                putRecordBatchResponseEntry.errorMessage()
                            )
                        }
                    }
                } else {
                    this.log.info("Successfully sent a batch of {} records. Failed records: {}", logs.size, failedCount)
                }
            }.exceptionally { e ->
                this.log.error("Failed to send a batch to Firehose.", e)
                null
            }
    }
}