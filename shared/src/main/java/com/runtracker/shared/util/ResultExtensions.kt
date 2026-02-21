package com.runtracker.shared.util

import android.util.Log
import kotlinx.coroutines.delay

private const val TAG = "ResultExtensions"

/**
 * Retries the given [block] with exponential backoff on failure.
 *
 * @param times Maximum number of retry attempts (default: 3)
 * @param initialDelayMs Initial delay before first retry (default: 100ms)
 * @param maxDelayMs Maximum delay between retries (default: 1000ms)
 * @param factor Multiplier for exponential backoff (default: 2.0)
 * @param block The suspend function to execute
 * @return Result containing the successful value or the last exception
 */
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): Result<T> {
    var currentDelay = initialDelayMs
    var lastException: Throwable? = null

    repeat(times) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Exception) {
            lastException = e
            Log.w(TAG, "Attempt ${attempt + 1}/$times failed: ${e.message}")

            if (attempt < times - 1) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    return Result.failure(lastException ?: Exception("Unknown error after $times retries"))
}

/**
 * Safely executes a database operation, catching any exceptions.
 *
 * @param operation The database operation to execute
 * @return Result containing the successful value or the exception
 */
suspend fun <T> safeDbOperation(operation: suspend () -> T): Result<T> {
    return try {
        Result.success(operation())
    } catch (e: Exception) {
        Log.e(TAG, "Database operation failed", e)
        Result.failure(e)
    }
}

/**
 * Safely executes a database operation with retry logic.
 *
 * @param times Maximum number of retry attempts
 * @param operation The database operation to execute
 * @return Result containing the successful value or the last exception
 */
suspend fun <T> safeDbOperationWithRetry(
    times: Int = 3,
    operation: suspend () -> T
): Result<T> {
    return retryWithBackoff(times = times, block = operation)
}
