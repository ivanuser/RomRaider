/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2026 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.external.phidget.interfacekit.io;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Verifies the documented Phidget InterfaceKit analog-input bit packing
 * (two 12-bit channels per three bytes). The byte-level decode is hardware
 * independent and so can be checked without a physical device.
 */
public class PhidgetIkDeviceTest {

    @Test
    public void decodesPackedTwelveBitPairs() {
        // ch0 = b0 + ((b1 & 0x0F) << 8)
        // ch1 = (b1 >> 4) + (b2 << 4)
        final byte[] report = new byte[16];
        report[0] = (byte) 0x23;
        report[1] = (byte) 0xC1; // low nibble -> ch0 high bits, high nibble -> ch1 low bits
        report[2] = (byte) 0xAB;

        final int[] values = PhidgetIkDevice.decodeAnalogInputs(report, 2);

        assertEquals(0x123, values[0]); // 291
        assertEquals(0xABC, values[1]); // 2748
    }

    @Test
    public void clampsToTwelveBitFullScale() {
        final byte[] report = new byte[16];
        for (int i = 0; i < report.length; i++) {
            report[i] = (byte) 0xFF;
        }
        final int[] values = PhidgetIkDevice.decodeAnalogInputs(report, 8);
        for (int v : values) {
            assertEquals(4095, v);
        }
    }

    @Test
    public void stopsCleanlyWhenReportTooShort() {
        final byte[] report = new byte[2]; // not enough bytes for a full pair
        final int[] values = PhidgetIkDevice.decodeAnalogInputs(report, 8);
        assertEquals(8, values.length); // returns array, leaves undecoded channels at 0
        assertEquals(0, values[7]);
    }

    @Test
    public void decodesAcrossMultiplePairsAndOddCount() {
        final byte[] report = new byte[16];
        // pair 0 -> ch0=0x001, ch1=0x234
        report[0] = (byte) 0x01;
        report[1] = (byte) 0x40;
        report[2] = (byte) 0x23;
        // pair 1 -> ch2=0xABC
        report[3] = (byte) 0xBC;
        report[4] = (byte) 0x0A;
        report[5] = (byte) 0x00;

        final int[] values = PhidgetIkDevice.decodeAnalogInputs(report, 3);

        assertEquals(0x001, values[0]);
        assertEquals(0x234, values[1]);
        assertEquals(0xABC, values[2]);
    }

    @Test
    public void parseSerialAcceptsDecoratedNumericStrings() throws Exception {
        final Method parseSerial = PhidgetIkDevice.class.getDeclaredMethod("parseSerial", String.class);
        parseSerial.setAccessible(true);

        assertEquals(12345, ((Integer) parseSerial.invoke(null, "12345")).intValue());
        assertEquals(12345, ((Integer) parseSerial.invoke(null, " SN-12345 ")).intValue());
    }

    @Test
    public void parseSerialReturnsMinusOneForNullOrNoDigits() throws Exception {
        final Method parseSerial = PhidgetIkDevice.class.getDeclaredMethod("parseSerial", String.class);
        parseSerial.setAccessible(true);

        assertEquals(-1, ((Integer) parseSerial.invoke(null, new Object[] { null })).intValue());
        assertEquals(-1, ((Integer) parseSerial.invoke(null, "serial: none")).intValue());
    }
}
