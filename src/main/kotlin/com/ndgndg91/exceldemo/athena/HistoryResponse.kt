package com.ndgndg91.exceldemo.athena

data class HistoryResponse(
    val data: List<Map<String, String>>,
    val nextCursor: Long?,
    val hasNext: Boolean
)