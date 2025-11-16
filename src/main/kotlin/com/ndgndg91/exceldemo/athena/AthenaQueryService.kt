package com.ndgndg91.exceldemo.athena

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaAsyncClient
import software.amazon.awssdk.services.athena.model.*
import java.util.concurrent.CompletableFuture

@Service
class AthenaQueryService(
    private val athenaClient: AthenaAsyncClient,
    @param:Value("\${aws.athena.database}") internal val database: String,
    @param:Value("\${aws.athena.workgroup}") private val workgroup: String,
    @param:Value("\${aws.athena.results-bucket}") private val resultsBucket: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun executeQuery(query: String): CompletableFuture<List<Map<String, String>>> {
        val queryExecutionContext = QueryExecutionContext.builder()
            .database(database)
            .build()

        val resultConfiguration = ResultConfiguration.builder()
            .outputLocation(resultsBucket)
            .build()

        val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
            .queryString(query)
            .queryExecutionContext(queryExecutionContext)
            .workGroup(workgroup)
            .resultConfiguration(resultConfiguration)
            .build()

        return athenaClient.startQueryExecution(startQueryExecutionRequest)
            .thenCompose { startResponse ->
                val queryExecutionId = startResponse.queryExecutionId()
                log.info("Started Athena query with execution ID: {}", queryExecutionId)
                waitForQueryToComplete(queryExecutionId)
            }
            .thenCompose { queryExecutionId ->
                getQueryResults(queryExecutionId)
            }
    }

    private fun waitForQueryToComplete(queryExecutionId: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        val thread = Thread {
            try {
                while (true) {
                    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
                        .queryExecutionId(queryExecutionId)
                        .build()
                    val getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest).join()
                    val state = getQueryExecutionResponse.queryExecution().status().state()
                    log.debug("Current query state for {}: {}", queryExecutionId, state)

                    if (state == QueryExecutionState.SUCCEEDED) {
                        future.complete(queryExecutionId)
                        break
                    } else if (state == QueryExecutionState.FAILED || state == QueryExecutionState.CANCELLED) {
                        val reason = getQueryExecutionResponse.queryExecution().status().stateChangeReason()
                        log.error("Query {} failed or was cancelled. Reason: {}", queryExecutionId, reason)
                        future.completeExceptionally(RuntimeException("Athena query failed. Final state: $state"))
                        break
                    }
                    Thread.sleep(1000) // Poll every second
                }
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        thread.start()
        return future
    }

    private fun getQueryResults(queryExecutionId: String): CompletableFuture<List<Map<String, String>>> {
        val getQueryResultsRequest = GetQueryResultsRequest.builder()
            .queryExecutionId(queryExecutionId)
            .build()

        return athenaClient.getQueryResults(getQueryResultsRequest).thenApply { resultsResponse ->
            val resultSet = resultsResponse.resultSet()
            val rows = resultSet.rows()
            if (rows.isEmpty()) {
                return@thenApply emptyList<Map<String, String>>()
            }

            val columnNames = rows.first().data().map { it.varCharValue() }
            val dataRows = rows.drop(1)

            dataRows.map { row ->
                row.data().mapIndexed { index, datum ->
                    columnNames[index] to datum.varCharValue()
                }.toMap()
            }
        }
    }
}