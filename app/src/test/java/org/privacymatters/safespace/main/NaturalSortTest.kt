package org.privacymatters.safespace.main

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class NaturalSortTest {

    private fun createItem(name: String) = Item(
        id = UUID.randomUUID(),
        name = name,
        size = 0,
        isDir = true,
        itemCount = "0",
        lastModified = 0,
        isSelected = false
    )

    private fun testBoth(names: List<String>, expectedAsc: List<String>) {
        val items = names.map { createItem(it) }
        
        // Test Ascending
        val actualAsc = items.sortedWith { o1, o2 -> naturalCompareAscending(o1, o2) }.map { it.name }
        assertEquals("Ascending sort failed for $names", expectedAsc, actualAsc)
        
        // Test Descending
        val actualDesc = items.sortedWith { o1, o2 -> naturalCompareDescending(o1, o2) }.map { it.name }
        // For natural sorting, descending should be the exact reverse of ascending
        assertEquals("Descending sort failed for $names", expectedAsc.reversed(), actualDesc)
    }

    @Test
    fun testUserReportedCase() {
        val names = listOf(
            "Aa", "Sa", "Sw", "da", "db", "dc", "dda", "ddb", "ddc", "de", "Sh", "Ta", "Zz"
        )
        val expected = listOf(
            "Aa", "da", "db", "dc", "dda", "ddb", "ddc", "de", "Sa", "Sh", "Sw", "Ta", "Zz"
        )
        testBoth(names, expected)
    }

    @Test
    fun testPrefix() {
        testBoth(listOf("ab", "a"), listOf("a", "ab"))
    }

    @Test
    fun testNumbers() {
        testBoth(listOf("file10", "file2", "file1"), listOf("file1", "file2", "file10"))
    }

    @Test
    fun testLeadingZeros() {
        // More leading zeros come first for same numeric value
        testBoth(listOf("file1", "file01", "file001"), listOf("file001", "file01", "file1"))
    }

    @Test
    fun testCaseInsensitivity() {
        testBoth(listOf("B", "a", "c"), listOf("a", "B", "c"))
    }

    @Test
    fun testAlphanumeric() {
        val names = listOf("10a", "1a", "2a", "a10", "a1", "a2", "1", "10", "2")
        // Numbers first, then alphanumeric starting with same prefix
        val expected = listOf("1", "1a", "2", "2a", "10", "10a", "a1", "a2", "a10")
        testBoth(names, expected)
    }

    @Test
    fun testVeryLargeNumbers() {
        val n1 = "9223372036854775808"
        val n2 = "9223372036854775809"
        val n3 = "123456789012345678901234567890"
        testBoth(listOf(n3, n2, n1), listOf(n1, n2, n3))
    }

    @Test
    fun testComplexAlphanumeric() {
        val names = listOf("v1.2.3", "v1.10.0", "v1.2.10", "v1.2.2")
        val expected = listOf("v1.2.2", "v1.2.3", "v1.2.10", "v1.10.0")
        testBoth(names, expected)
    }

    @Test
    fun testEdgeCases() {
        // Empty strings
        testBoth(listOf("a", "", "b"), listOf("", "a", "b"))
        
        // Special characters
        testBoth(listOf("a-b", "a b", "a_b"), listOf("a b", "a-b", "a_b"))
        
        // Numeric strings with non-digit separators
        testBoth(listOf("10.1", "10.01", "10.001"), listOf("10.001", "10.01", "10.1"))
        
        // Strings that are purely numbers vs strings starting with numbers
        testBoth(listOf("100", "10"), listOf("10", "100"))
        
        // Mixed case with numbers
        testBoth(listOf("A1", "a01", "A001"), listOf("A001", "a01", "A1"))
    }

    @Test
    fun testLongNumericRuns() {
        val s1 = "1" + "0".repeat(100)
        val s2 = "1" + "0".repeat(101)
        testBoth(listOf(s2, s1), listOf(s1, s2))
    }
}
