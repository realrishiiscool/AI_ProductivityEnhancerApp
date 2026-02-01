package com.example.ai_productivityenhancer

import kotlinx.coroutines.delay

object AiApi {

    suspend fun getAiRules(): Map<String, Float> {
        // Simulate network latency
        delay(1000)

        // Return a mock set of rules
        return mapOf(
            "Entertainment" to 1.5f, // 1.5 hours
            "Gaming" to 0.5f       // 0.5 hours
        )
    }
}
