package org.privacymatters.safespace.lib.fileManager

/*
    With Help From:
    https://github.com/ritwik12/NaturalSort/blob/master/NaturalSort.java
 */

fun compareRight(s1: String, s2: String): Int {
    var temp = 0
    var iS1 = 0
    var iS2 = 0

    while (true) {
        val charS1 = charAt(s1, iS1)
        val charS2 = charAt(s2, iS2)

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
        iS1++
        iS2++
    }
}

fun naturalCompareAscending(o1: FileItem, o2: FileItem): Int {
    val s1 = o1.name
    val s2 = o2.name
    
    var iS1 = 0
    var iS2 = 0

    var nzS1: Int
    var nzS2: Int

    var charS1: Char
    var charS2: Char

    while (true) {
        // Only count the number of zeroes leading the last number compared
        nzS2 = 0
        nzS1 = 0

        charS1 = charAt(s1, iS1)
        charS2 = charAt(s2, iS2)

        // skip over leading spaces or zeros
        while (Character.isSpaceChar(charS1) || charS1 == '0') {
            if (charS1 == '0') {
                nzS1++
            } else {
                // Only count consecutive zeroes
                nzS1 = 0
            }
            charS1 = charAt(s1, ++iS1)
        }
        while (Character.isSpaceChar(charS2) || charS2 == '0') {
            if (charS2 == '0') {
                nzS2++
            } else {
                // Only count consecutive zeroes
                nzS2 = 0
            }
            charS2 = charAt(s2, ++iS2)
        }

        // Process run of digits
        if (Character.isDigit(charS1) && Character.isDigit(charS2)) {
            val temp = compareRight(s1.substring(iS1), s2.substring(iS2))
            if (temp != 0) {
                return temp
            }
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return nzS1 - nzS2
        }
        if (charS1 < charS2) {
            return -1
        }
        if (charS1 > charS2) {
            return +1
        }
        ++iS1
        ++iS2
    }
}


fun compareLeft(s1: String, s2: String): Int {
    var temp = 0
    var iS1 = 0
    var iS2 = 0

    while (true) {
        val charS1 = charAt(s1, iS1)
        val charS2 = charAt(s2, iS2)

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
        iS1++
        iS2++
    }
}

fun naturalCompareDescending(o1: FileItem, o2: FileItem): Int {
    val s1 = o1.name
    val s2 = o2.name

    var iS1 = 0
    var iS2 = 0

    var nzS1: Int
    var nzS2: Int

    var charS1: Char
    var charS2: Char

    while (true) {
        // Only count the number of zeroes leading the last number compared
        nzS2 = 0
        nzS1 = 0

        charS1 = charAt(s1, iS1)
        charS2 = charAt(s2, iS2)

        // skip over leading spaces or zeros
        while (Character.isSpaceChar(charS1) || charS1 == '0') {
            if (charS1 == '0') {
                nzS1++
            } else {
                // Only count consecutive zeroes
                nzS1 = 0
            }
            charS1 = charAt(s1, ++iS1)
        }
        while (Character.isSpaceChar(charS2) || charS2 == '0') {
            if (charS2 == '0') {
                nzS2++
            } else {
                // Only count consecutive zeroes
                nzS2 = 0
            }
            charS2 = charAt(s2, ++iS2)
        }

        // Process run of digits
        if (Character.isDigit(charS1) && Character.isDigit(charS2)) {
            val temp = compareLeft(s1.substring(iS1), s2.substring(iS2))
            if (temp != 0) {
                return temp
            }
        }
        if (charS1.code == 0 && charS2.code == 0) {
            return nzS1 - nzS2
        }
        if (charS1 < charS2) {
            return +1
        }
        if (charS1 > charS2) {
            return -1
        }
        ++iS1
        ++iS2
    }
}


fun charAt(s: String, i: Int): Char {
    return if (i >= s.length) '0' else s[i]
}

