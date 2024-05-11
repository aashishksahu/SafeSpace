package org.privacymatters.safespace.experimental.mainn

/*
    With Help From:
    https://github.com/ritwik12/NaturalSort/blob/master/NaturalSort.java
 */

fun compareRight(s1: String, s2: String): Int {
    var temp = 0
    var strIndex1 = 0
    var strIndex2 = 0

    while (true) {
        val charS1 = charAt(s1, strIndex1)
        val charS2 = charAt(s2, strIndex2)

        if (!Character.isDigit(charS1) && !Character.isDigit(charS2)) {
            return temp
        }
        if (!Character.isDigit(charS1)) {
            return -1
        }
        if (!Character.isDigit(charS2)) {
            return 1
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return temp
        }
        if (temp == 0) {
            if (charS1 < charS2) {
                temp = -1
            } else if (charS1 > charS2) {
                temp = 1
            }
        }
        strIndex1++
        strIndex2++
    }
}

fun naturalCompareAscending(o1: Item, o2: Item): Int {
    val s1 = o1.name
    val s2 = o2.name

    var strIndex1 = 0
    var strIndex2 = 0

    var numZeroS1: Int
    var numZeroS2: Int

    var charS1: Char
    var charS2: Char

    while (true) {
        // Only count the number of zeroes leading the last number compared
        numZeroS2 = 0
        numZeroS1 = 0

        try {
            charS1 = charAt(s1, strIndex1)
            charS2 = charAt(s2, strIndex2)
        } catch (exp: StringIndexOutOfBoundsException) {
            break
        }

        // skip over leading spaces or zeros
        while (Character.isSpaceChar(charS1) || charS1 == '0') {
            if (charS1 == '0') {
                numZeroS1++
            } else {
                // Only count consecutive zeroes
                numZeroS1 = 0
            }
            charS1 = charAt(s1, ++strIndex1)
        }
        while (Character.isSpaceChar(charS2) || charS2 == '0') {
            if (charS2 == '0') {
                numZeroS2++
            } else {
                // Only count consecutive zeroes
                numZeroS2 = 0
            }
            charS2 = charAt(s2, ++strIndex2)
        }

        // Process run of digits
        if (Character.isDigit(charS1) && Character.isDigit(charS2)) {
            val temp = compareRight(s1.substring(strIndex1), s2.substring(strIndex2))
            if (temp != 0) {
                return temp
            }
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return numZeroS1 - numZeroS2
        }
        if (charS1 < charS2) {
            return -1
        }
        if (charS1 > charS2) {
            return +1
        }
        ++strIndex1
        ++strIndex2
    }
    return 0
}


fun compareLeft(s1: String, s2: String): Int {
    var temp = 0
    var strIndex1 = 0
    var strIndex2 = 0

    while (true) {
        val charS1 = charAt(s1, strIndex1)
        val charS2 = charAt(s2, strIndex2)

        if (!Character.isDigit(charS1) && !Character.isDigit(charS2)) {
            return temp
        }
        if (!Character.isDigit(charS1)) {
            return 1
        }
        if (!Character.isDigit(charS2)) {
            return -1
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return temp
        }
        if (temp == 0) {
            if (charS1 < charS2) {
                temp = 1
            } else if (charS1 > charS2) {
                temp = -1
            }
        }
        strIndex1++
        strIndex2++
    }
}

fun naturalCompareDescending(o1: Item, o2: Item): Int {
    val s1 = o1.name
    val s2 = o2.name

    var strIndex1 = 0
    var strIndex2 = 0

    var numZeroS1: Int
    var numZeroS2: Int

    var charS1: Char
    var charS2: Char

    while (true) {
        // Only count the number of zeroes leading the last number compared
        numZeroS1 = 0
        numZeroS2 = 0

        try {
            charS1 = charAt(s1, strIndex1)
            charS2 = charAt(s2, strIndex2)
        } catch (exp: StringIndexOutOfBoundsException) {
            break
        }

        // skip over leading spaces or zeros
        while (Character.isSpaceChar(charS1) || charS1 == '0') {
            if (charS1 == '0') {
                numZeroS1++
            } else {
                // Only count consecutive zeroes
                numZeroS1 = 0
            }
            charS1 = charAt(s1, ++strIndex1)
        }
        while (Character.isSpaceChar(charS2) || charS2 == '0') {
            if (charS2 == '0') {
                numZeroS2++
            } else {
                // Only count consecutive zeroes
                numZeroS2 = 0
            }
            charS2 = charAt(s2, ++strIndex2)
        }

        // Process run of digits
        if (Character.isDigit(charS1) && Character.isDigit(charS2)) {
            val temp = compareLeft(s1.substring(strIndex1), s2.substring(strIndex2))
            if (temp != 0) {
                return temp
            }
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return numZeroS1 - numZeroS2
        }
        if (charS1 < charS2) {
            return +1
        }
        if (charS1 > charS2) {
            return -1
        }
        ++strIndex1
        ++strIndex2
    }
    return 0
}


fun charAt(s: String, i: Int): Char {
    return if (i >= s.length) throw StringIndexOutOfBoundsException() else s[i]
}