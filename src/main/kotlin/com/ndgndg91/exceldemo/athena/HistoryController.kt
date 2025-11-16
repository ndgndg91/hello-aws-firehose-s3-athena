package com.ndgndg91.exceldemo.athena

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@RestController
class HistoryController(private val athenaQueryService: AthenaQueryService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_LIMIT = 5
    }

    @GetMapping("/history/{accountId}")
    fun getHistory(
        @PathVariable accountId: Long,
        @RequestParam(required = true) period: String, // e.g., 3m, 6m, 12m, 36m
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(required = false, defaultValue = "$DEFAULT_LIMIT") limit: Int
    ): CompletableFuture<ResponseEntity<Any>> {

        val now = LocalDate.now()
        val startDate = when (period) {
            "3m" -> now.minusMonths(3)
            "6m" -> now.minusMonths(6)
            "12m" -> now.minusMonths(12)
            "36m" -> now.minusMonths(36)
            else -> return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body("Invalid period. Use '3m', '6m', '12m', or '36m'.")
            )
        }

        val cursorClause = cursor?.let { "AND created_at < $it" } ?: ""
        val queryLimit = limit + 1 // Fetch one more item to check if there is a next page

        // Efficient query using the 'dt' partition key and quoting the database name
        val query = """
            SELECT * FROM "${athenaQueryService.database}"."user_history_logs"
            WHERE 
                dt BETWEEN '$startDate' AND '$now'
                AND account_id = $accountId
                $cursorClause
            ORDER BY created_at DESC
            LIMIT $queryLimit
        """.trimIndent()

        logger.info("{}", query)

        return athenaQueryService.executeQuery(query)
            .thenApply { results ->
                var nextCursor: Long? = null
                val hasNext = results.size > limit
                val data = if (hasNext) results.take(limit) else results

                if (hasNext) {
                    nextCursor = data.lastOrNull()?.get("created_at")?.toLongOrNull()
                }

                val response = HistoryResponse(data = data, nextCursor = nextCursor, hasNext = hasNext)
                ResponseEntity.ok(response as Any)
            }
            .exceptionally { e -> ResponseEntity.internalServerError().body(e.message as Any) }
    }
}