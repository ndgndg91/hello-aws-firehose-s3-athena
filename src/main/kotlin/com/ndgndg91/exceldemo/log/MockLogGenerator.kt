package com.ndgndg91.exceldemo.log

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ThreadLocalRandom

@Service
class MockLogGenerator {

    private val logTypes = listOf("LOGIN", "LOGOUT", "PURCHASE", "VIEW_ITEM", "ADD_TO_CART", "CHECKOUT")
    private val startEpochDay = LocalDate.of(2025, 1, 1).toEpochDay()

    fun generate(): UserHistoryLog {
        val accountId = ThreadLocalRandom.current().nextLong(1, 4_000_001)
        val type = logTypes.random()

        // 1. Generate a random date
        val todayEpochDay = LocalDate.now().toEpochDay()
        val randomEpochDay = ThreadLocalRandom.current().nextLong(startEpochDay, todayEpochDay + 1)
        val randomDate = LocalDate.ofEpochDay(randomEpochDay)

        // 2. Generate a random millisecond timestamp within that random date
        val startOfDaySeconds = randomDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val endOfDaySeconds = randomDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) - 1
        val randomTimestampSeconds = ThreadLocalRandom.current().nextLong(startOfDaySeconds, endOfDaySeconds + 1)
        val randomMillis = ThreadLocalRandom.current().nextLong(0, 1000)
        val randomCreatedAt = (randomTimestampSeconds * 1000) + randomMillis

        val contextData = when (type) {
            "PURCHASE" -> mapOf("item_id" to "item-${ThreadLocalRandom.current().nextInt(1, 1000)}", "price" to "${ThreadLocalRandom.current().nextInt(1000, 100000)}")
            "VIEW_ITEM" -> mapOf("item_id" to "item-${ThreadLocalRandom.current().nextInt(1, 1000)}")
            else -> mapOf("device" to listOf("web", "mobile").random(), "ip_address" to "192.168.${ThreadLocalRandom.current().nextInt(0, 256)}.${ThreadLocalRandom.current().nextInt(1, 255)}")
        }

        // 3. Create the log. 'dt' will be computed automatically from 'createdAt'.
        return UserHistoryLog(
            accountId = accountId,
            type = type,
            createdAt = randomCreatedAt,
            contextData = contextData
        )
    }
}