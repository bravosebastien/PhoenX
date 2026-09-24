package com.example.phoenx.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipientCsvTest {

    private fun cleanRecipientIds(csv: String): String {
        return csv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    @Test
    fun testDuplicatesDeduplicated() {
        val input = "id1,id1,id1"
        val result = cleanRecipientIds(input)
        assertEquals("id1", result)
    }

    @Test
    fun testSpacesAndEmptyItemsCleaned() {
        val input = "id1, ,id2,"
        val result = cleanRecipientIds(input)
        assertEquals("id1,id2", result)
    }

    @Test
    fun testEmptyInputReturnsEmpty() {
        val input = ""
        val result = cleanRecipientIds(input)
        assertEquals("", result)
    }

    @Test
    fun testSingleItemReturnsSame() {
        val input = "id1"
        val result = cleanRecipientIds(input)
        assertEquals("id1", result)
    }
}
