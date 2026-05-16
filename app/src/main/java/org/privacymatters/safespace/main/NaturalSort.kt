package org.privacymatters.safespace.main

import java.math.BigInteger

/**
 * Robust natural sort comparison.
 * Handles digits as numbers and other characters case-insensitively.
 */
private fun compareNatural(s1: String, s2: String): Int {
    var i = 0
    var j = 0

    while (i < s1.length && j < s2.length) {
        val c1 = s1[i]
        val c2 = s2[j]

        if (Character.isDigit(c1) && Character.isDigit(c2)) {
            val num1Start = i
            while (i < s1.length && Character.isDigit(s1[i])) i++
            val num1Str = s1.substring(num1Start, i)

            val num2Start = j
            while (j < s2.length && Character.isDigit(s2[j])) j++
            val num2Str = s2.substring(num2Start, j)

            // Compare numeric values
            val n1 = BigInteger(num1Str)
            val n2 = BigInteger(num2Str)
            val cmp = n1.compareTo(n2)
            if (cmp != 0) return cmp

            // If numeric values are equal, the one with more leading zeros comes first (shorter string first)
            if (num1Str.length != num2Str.length) {
                return num2Str.length - num1Str.length
            }
        } else {
            val cmp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
            if (cmp != 0) return cmp
            i++
            j++
        }
    }

    return s1.length - s2.length
}

fun naturalCompareAscending(o1: Item, o2: Item): Int {
    return compareNatural(o1.name, o2.name)
}

fun naturalCompareDescending(o1: Item, o2: Item): Int {
    return -compareNatural(o1.name, o2.name)
}
