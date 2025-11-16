package com.ndgndg91.exceldemo.log

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UserHistoryLogSerializationTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }

    @Test
    fun `should serialize UserHistoryLog to snake_case JSON with computed dt`() {
        // Given
        val fixedCreatedAtMillis = Instant.parse("2025-11-16T10:00:00.000Z").toEpochMilli()
        val log = UserHistoryLog(
            accountId = 12345L,
            type = "LOGIN",
            createdAt = fixedCreatedAtMillis,
            contextData = mapOf("device" to "mobile", "ip_address" to "192.168.0.1")
        )

        // When
        val json = objectMapper.writeValueAsString(log)

        // Then
        assertThat(json).contains("\"account_id\":12345")
        assertThat(json).contains("\"type\":\"LOGIN\"")
        assertThat(json).contains("\"created_at\":$fixedCreatedAtMillis")
        assertThat(json).contains("\"context_data\":{\"device\":\"mobile\",\"ip_address\":\"192.168.0.1\"}")
        assertThat(json).contains("\"dt\":\"2025-11-16\"") // Derived from fixedCreatedAtMillis
        assertThat(json).doesNotContain("accountId") // Ensure camelCase is not in JSON
        assertThat(json).doesNotContain("createdAt")
        assertThat(json).doesNotContain("contextData")
    }

    @Test
    fun `should deserialize snake_case JSON to UserHistoryLog`() {
        // Given
        val json = """
            {
                "account_id": 67890,
                "type": "PURCHASE",
                "created_at": 1763291614000,
                "context_data": {"item_id":"item-001","price":"15000"},
                "dt": "2025-11-16"
            }
        """.trimIndent()

        // When
        val log = objectMapper.readValue<UserHistoryLog>(json)

        // Then
        assertThat(log.accountId).isEqualTo(67890L)
        assertThat(log.type).isEqualTo("PURCHASE")
        assertThat(log.createdAt).isEqualTo(1763291614000L)
        assertThat(log.contextData).isEqualTo(mapOf("item_id" to "item-001", "price" to "15000"))
        assertThat(log.dt).isEqualTo("2025-11-16") // Ensure computed dt matches
    }

    @Test
    fun `should handle default createdAt and computed dt when not provided`() {
        // Given
        val log = UserHistoryLog(
            accountId = 11111L,
            type = "LOGOUT",
            contextData = mapOf("reason" to "session_timeout")
        )

        // When
        val json = objectMapper.writeValueAsString(log)

        // Then
        assertThat(json).contains("\"account_id\":11111")
        assertThat(json).contains("\"type\":\"LOGOUT\"")
        assertThat(json).contains("\"context_data\":{\"reason\":\"session_timeout\"}")
        // created_at and dt will be current time, so we check for presence and format
        assertThat(json).containsPattern("""created_at":\d{13}""") // 13 digits for milliseconds
        assertThat(json).containsPattern("""dt":"\d{4}-\d{2}-\d{2}"""") // YYYY-MM-DD format

        val deserializedLog = objectMapper.readValue<UserHistoryLog>(json)
        assertThat(deserializedLog.accountId).isEqualTo(11111L)
        assertThat(deserializedLog.type).isEqualTo("LOGOUT")
        assertThat(deserializedLog.contextData).isEqualTo(mapOf("reason" to "session_timeout"))
        assertThat(deserializedLog.createdAt).isNotNull()
        assertThat(deserializedLog.dt).isEqualTo(LocalDate.ofInstant(Instant.ofEpochMilli(deserializedLog.createdAt), ZoneOffset.UTC).toString())
    }
}
