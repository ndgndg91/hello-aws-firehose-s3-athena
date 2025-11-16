package com.ndgndg91.exceldemo.log

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UserHistoryLog(
    @field:JsonProperty("account_id")
    val accountId: Long,
    val type: String,
    @field:JsonProperty("created_at")
    val createdAt: Long = Instant.now().toEpochMilli(),
    @field:JsonProperty("context_data")
    val contextData: Map<String, String>
) {
    /**
     * A computed property for the partition key 'dt'.
     * It is automatically derived from the 'createdAt' timestamp and serialized to JSON.
     * The date is calculated based on Korea Standard Time (Asia/Seoul).
     */
    @get:JsonProperty("dt", access = JsonProperty.Access.READ_ONLY)
    val dt: String
        get() = LocalDate.ofInstant(Instant.ofEpochMilli(this.createdAt), ZoneId.of("Asia/Seoul")).toString()
}