package com.powertracker.ui.today

import com.powertracker.ui.today.EntryKind.KG
import com.powertracker.ui.today.EntryKind.REPS
import com.powertracker.ui.today.EntryKind.RPE
import com.powertracker.ui.today.EntryResult.Error.EMPTY
import com.powertracker.ui.today.EntryResult.Error.NAN
import com.powertracker.ui.today.EntryResult.Error.RANGE
import com.powertracker.ui.today.EntryResult.Ok
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryTest {
    @Test
    fun readsACommaAsTheDecimalPoint() {
        assertEquals(Ok(102.5), parseEntry("102,5", KG))
        assertEquals(Ok(102.5), parseEntry(" 102.5 ", KG))
        assertEquals(Ok(0.5), parseEntry(",5", KG))
        assertEquals(Ok(102.0), parseEntry("102,", KG))
    }

    @Test
    fun refusesWhatIsNotANumber() {
        assertEquals(NAN, parseEntry("abc", KG))
        assertEquals(NAN, parseEntry("1.2.3", KG))
        assertEquals(NAN, parseEntry("1,2,3", KG))
        assertEquals(NAN, parseEntry("1e3", KG))
        assertEquals(NAN, parseEntry(".", RPE))
        assertEquals(NAN, parseEntry(",", RPE))
        assertEquals(NAN, parseEntry("1 0", REPS))
    }

    @Test
    fun refusesALeadingSign() {
        assertEquals(NAN, parseEntry("-5", KG))
        assertEquals(NAN, parseEntry("-0", KG))
        assertEquals(NAN, parseEntry("+5", KG))
        assertEquals(NAN, parseEntry("-5", REPS))
        assertEquals(NAN, parseEntry("-8", RPE))
    }

    @Test
    fun refusesACommaUsedAsAThousandsSeparator() {
        // "1,000" is a thousand to whoever typed it, never 1 kg.
        assertEquals(NAN, parseEntry("1,000", KG))
        assertEquals(NAN, parseEntry("12,500", KG))
        assertEquals(NAN, parseEntry("1,000", REPS))
        // Two or four digits after the comma are decimals.
        assertEquals(Ok(102.5), parseEntry("102,50", KG))
        assertEquals(Ok(1.0), parseEntry("1,0000", KG))
        // A point is always the decimal point.
        assertEquals(Ok(1.0), parseEntry("1.000", KG))
    }

    @Test
    fun keepsWeightWithin0To1000Kg_twoDecimalsAtMost() {
        assertEquals(RANGE, parseEntry("1200", KG))
        assertEquals(RANGE, parseEntry("1000.01", KG))
        assertEquals(Ok(0.0), parseEntry("0", KG))
        assertEquals(Ok(1000.0), parseEntry("1000", KG))
        assertEquals(Ok(61.26), parseEntry("61.255", KG))
        assertEquals(Ok(61.25), parseEntry("61.2549", KG))
    }

    @Test
    fun takesWholeRepsFrom1To99() {
        assertEquals(RANGE, parseEntry("0", REPS))
        assertEquals(RANGE, parseEntry("100", REPS))
        assertEquals(RANGE, parseEntry("5.5", REPS))
        assertEquals(RANGE, parseEntry("5,5", REPS))
        assertEquals(Ok(1.0), parseEntry("1", REPS))
        assertEquals(Ok(99.0), parseEntry("99", REPS))
    }

    @Test
    fun keepsRpeWithin6To10_andSnapsItToTheHalf() {
        assertEquals(RANGE, parseEntry("11", RPE))
        assertEquals(RANGE, parseEntry("5.5", RPE))
        assertEquals(Ok(8.5), parseEntry("8.3", RPE))
        assertEquals(Ok(8.0), parseEntry("8,2", RPE))
        assertEquals(Ok(10.0), parseEntry("10", RPE))
        assertEquals(Ok(6.0), parseEntry("6", RPE))
    }

    @Test
    fun callsAnEmptyFieldEmpty_whateverItIsFor() {
        for (kind in EntryKind.entries) assertEquals(EMPTY, parseEntry("", kind))
        assertEquals(EMPTY, parseEntry("  ", KG))
    }

    @Test
    fun aNumberTooLongToHoldIsOutOfRange_notACrash() {
        assertEquals(RANGE, parseEntry("9".repeat(400), KG))
        assertEquals(RANGE, parseEntry("9".repeat(400), REPS))
    }

    @Test
    fun entryMessage_saysWhatTheFieldTakes_andNothingForAnEmptyOne() {
        assertEquals("Numbers only", entryMessage(KG, NAN))
        assertEquals("0 to 1000 kg", entryMessage(KG, RANGE))
        assertEquals("Whole, 1 to 99", entryMessage(REPS, RANGE))
        assertEquals("6 to 10", entryMessage(RPE, RANGE))
        assertNull(entryMessage(RPE, EMPTY))
    }

    @Test
    fun plate_whole() {
        assertEquals(Ok(7.0), parseEntry("7", EntryKind.PLATE))
        assertEquals(Ok(50.0), parseEntry("50", EntryKind.PLATE))
        assertEquals(EntryResult.Error.STEP, parseEntry("7.5", EntryKind.PLATE))
        assertEquals("Whole plates only", entryMessage(EntryKind.PLATE, EntryResult.Error.STEP))
        assertEquals(RANGE, parseEntry("0", EntryKind.PLATE))
        assertEquals(RANGE, parseEntry("51", EntryKind.PLATE))
        assertEquals("Plate 1 to 50", entryMessage(EntryKind.PLATE, RANGE))
        assertEquals(NAN, parseEntry("abc", EntryKind.PLATE))
    }

    @Test
    fun plate_half() {
        assertEquals(Ok(7.5), parseEntry("7.5", EntryKind.PLATE_HALF))
        assertEquals(Ok(7.5), parseEntry("7,5", EntryKind.PLATE_HALF))
        assertEquals(Ok(7.0), parseEntry("7", EntryKind.PLATE_HALF))
        assertEquals(EntryResult.Error.STEP, parseEntry("7.25", EntryKind.PLATE_HALF))
        assertEquals("Whole or half plates", entryMessage(EntryKind.PLATE_HALF, EntryResult.Error.STEP))
        assertEquals(RANGE, parseEntry("0.5", EntryKind.PLATE_HALF))
        assertEquals("Plate 1 to 50", entryMessage(EntryKind.PLATE_HALF, RANGE))
    }
}
