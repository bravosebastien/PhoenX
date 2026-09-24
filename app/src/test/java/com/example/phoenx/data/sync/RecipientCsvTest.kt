package com.example.phoenx.data.sync

import com.example.phoenx.domain.util.RecipientUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipientCsvTest {

    @Test
    fun testDuplicatesDeduplicated() {
        val input = "id1,id1,id1"
        val result = RecipientUtils.cleanRecipientIds(input)
        assertEquals("id1", result)
    }

    @Test
    fun testSpacesAndEmptyItemsCleaned() {
        val input = "id1, ,id2,"
        val result = RecipientUtils.cleanRecipientIds(input)
        assertEquals("id1,id2", result)
    }

    @Test
    fun testEmptyInputReturnsEmpty() {
        val input = ""
        val result = RecipientUtils.cleanRecipientIds(input)
        assertEquals("", result)
    }

    @Test
    fun testSingleItemReturnsSame() {
        val input = "id1"
        val result = RecipientUtils.cleanRecipientIds(input)
        assertEquals("id1", result)
    }
}
