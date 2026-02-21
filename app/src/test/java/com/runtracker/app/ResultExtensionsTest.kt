package com.runtracker.app

import com.runtracker.shared.util.retryWithBackoff
import com.runtracker.shared.util.safeDbOperation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResultExtensionsTest {

    @Test
    fun `retryWithBackoff succeeds on first attempt`() = runTest {
        var attempts = 0

        val result = retryWithBackoff(times = 3) {
            attempts++
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }

    @Test
    fun `retryWithBackoff retries on failure then succeeds`() = runTest {
        var attempts = 0

        val result = retryWithBackoff(times = 3, initialDelayMs = 10) {
            attempts++
            if (attempts < 3) {
                throw Exception("Attempt $attempts failed")
            }
            "success on attempt $attempts"
        }

        assertTrue(result.isSuccess)
        assertEquals("success on attempt 3", result.getOrNull())
        assertEquals(3, attempts)
    }

    @Test
    fun `retryWithBackoff returns failure after retries exhausted`() = runTest {
        var attempts = 0

        val result = retryWithBackoff(times = 3, initialDelayMs = 10) {
            attempts++
            throw Exception("Always fails")
        }

        assertTrue(result.isFailure)
        assertEquals(3, attempts)
        assertEquals("Always fails", result.exceptionOrNull()?.message)
    }

    @Test
    fun `retryWithBackoff respects custom retry count`() = runTest {
        var attempts = 0

        val result = retryWithBackoff(times = 5, initialDelayMs = 10) {
            attempts++
            throw Exception("Fail")
        }

        assertTrue(result.isFailure)
        assertEquals(5, attempts)
    }

    @Test
    fun `safeDbOperation catches exceptions`() = runTest {
        val result = safeDbOperation {
            throw IllegalStateException("Database error")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `safeDbOperation returns success for successful operations`() = runTest {
        val result = safeDbOperation {
            42
        }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `safeDbOperation handles null return values`() = runTest {
        val result = safeDbOperation<String?> {
            null
        }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `retryWithBackoff with single attempt fails immediately`() = runTest {
        var attempts = 0

        val result = retryWithBackoff(times = 1, initialDelayMs = 10) {
            attempts++
            throw Exception("Fail")
        }

        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }
}
