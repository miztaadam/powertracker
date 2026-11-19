package com.powertracker.domain.motion

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `fixed()` byte for byte, on the JVM and in Kotlin/JS: its Int path (`bf6ec53`) and the Long path above Int's range
 * must print the same strings. The expected strings are written out by hand from the rule the function uses,
 * `floor(|v| * 10^digits + 0.5)`, half away from zero on the double's own value.
 *
 * Two places where that rule is not JavaScript's `toFixed` (which works on the exact decimal value and keeps the sign
 * of a negative that rounds to zero) are pinned as the function does them: 1.45 at 1 digit is "1.5" here ("1.4" in
 * `toFixed`), and -0.004 at 2 digits is "0.00" here ("-0.00" in `toFixed`). No shape reaches either case; the seeded
 * shape tests compare against the reference page's own path strings.
 */
class FixedFormatTest {
    private fun pin(expected: String, v: Double, digits: Int) = assertEquals(expected, fixed(v, digits), "fixed($v, $digits)")

    @Test
    fun wholeNumbers_andTiesAwayFromZero() {
        pin("0", 0.0, 0)
        pin("4", 3.7, 0)
        pin("1", 0.5, 0)
        pin("3", 2.5, 0)
        pin("-3", -2.5, 0)
        pin("-12", -12.4, 0)
    }

    @Test
    fun theFractionIsPaddedWithZeros() {
        pin("0.00", 0.0, 2)
        pin("5.000", 5.0, 3)
        pin("0.05", 0.05, 2)
        pin("1.000", 1.0001, 3)
        pin("0.00", 0.001, 2)
        pin("7.08", 7.08, 2)
    }

    @Test
    fun roundingEdges() {
        pin("0.13", 0.125, 2) // an exact tie in binary, away from zero
        pin("0.1", 0.05, 1)
        pin("1.00", 1.005, 2) // 1.005 is stored a little under, so it rounds down
        pin("9.99", 9.995, 2) // likewise
        pin("10.00", 9.9951, 2) // a carry into the whole part
        pin("1.0", 0.95, 1)
        pin("123.5", 123.456, 1)
        pin("8.35", 8.345, 2)
        pin("1.5", 1.45, 1) // toFixed gives "1.4" here (see the class note)
    }

    @Test
    fun negatives() {
        pin("-12.346", -12.3456, 3)
        pin("-0.01", -0.005, 2)
        pin("-0.5", -0.5, 1)
        pin("-199.90", -199.9, 2)
        pin("0.00", -0.004, 2) // rounds to zero: no sign (toFixed keeps "-0.00")
        pin("0", -0.4, 0)
    }

    @Test
    fun theIntAndLongPathsMeet() {
        pin("2147483646", 2147483646.0, 0) // the last value on the Int path
        pin("2147483647", 2147483647.0, 0) // Int.MAX_VALUE itself goes the Long way
        pin("2147483648", 2147483648.0, 0)
        pin("-2147483648", -2147483648.0, 0)
        pin("21474836.46", 21474836.46, 2) // just under Int's range once scaled
        pin("30000000.13", 30000000.125, 2) // the Long path with digits
        pin("-300000000.3", -300000000.25, 1)
        pin("40000000.05", 40000000.05, 2)
    }
}
