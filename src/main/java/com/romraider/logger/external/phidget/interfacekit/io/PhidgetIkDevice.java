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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hid4java.HidDevice;

/**
 * A native-free driver for a Phidget InterfaceKit, built on hid4java (which
 * bundles hidapi for every platform).  This replaces the end-of-life
 * phidget21 JNI wrapper, removing the need for users to install Phidget's
 * native system library.
 *
 * <p>It reads the InterfaceKit's analog inputs straight from the USB HID
 * input report and decodes them per the documented Phidget protocol (8 analog
 * channels, 12-bit, packed two per three bytes; raw 0-4095 scaled to the
 * 0-1000 "sensor value" the rest of RomRaider expects).</p>
 *
 * <p><b>Hardware validation note:</b> the analog report layout follows the
 * published Phidget InterfaceKit HID protocol, but the exact report offset can
 * vary slightly by device revision. It compiles and enumerates without
 * hardware; the decoded values should be confirmed against a physical
 * InterfaceKit. See {@link #decodeAnalogInputs(byte[], int)}.</p>
 */
public final class PhidgetIkDevice {

    private static final Logger LOGGER = Logger.getLogger(PhidgetIkDevice.class);

    /** USB vendor id assigned to Phidgets Inc. */
    public static final int VENDOR_ID = 0x06C2;

    /** 12-bit ADC full scale; raw analog values run 0..ADC_MAX. */
    private static final int ADC_MAX = 4095;

    /** Phidget "sensor value" full scale (what getSensorValue() returned). */
    private static final int SENSOR_VALUE_MAX = 1000;

    /** Known InterfaceKit product ids -> (display name, analog input count). */
    private static final Map<Integer, IkProduct> PRODUCTS;
    static {
        final Map<Integer, IkProduct> p = new HashMap<Integer, IkProduct>();
        p.put(0x0040, new IkProduct("PhidgetInterfaceKit 0/16/16", 0));
        p.put(0x0044, new IkProduct("PhidgetInterfaceKit 0/0/4", 0));
        p.put(0x0045, new IkProduct("PhidgetInterfaceKit 8/8/8", 8));
        p.put(0x0051, new IkProduct("PhidgetInterfaceKit 0/0/8", 0));
        p.put(0x0053, new IkProduct("PhidgetInterfaceKit 8/8/8", 8));
        p.put(0x004F, new IkProduct("PhidgetInterfaceKit 0/8/8/8", 8));
        PRODUCTS = Collections.unmodifiableMap(p);
    }

    private final HidDevice device;
    private final int serial;
    private final String name;
    private final int sensorCount;

    PhidgetIkDevice(HidDevice device) {
        this.device = device;
        this.serial = parseSerial(device.getSerialNumber());
        final IkProduct product = PRODUCTS.get(device.getProductId());
        this.name = product != null ? product.name : device.getProduct();
        // default to 8 analog inputs for an unrecognised InterfaceKit
        this.sensorCount = product != null && product.analogInputs > 0
                ? product.analogInputs : 8;
    }

    /** True if the given HID device looks like a Phidget InterfaceKit. */
    public static boolean isInterfaceKit(HidDevice device) {
        return device.getVendorId() == VENDOR_ID
                && (PRODUCTS.containsKey(device.getProductId())
                        || (device.getProduct() != null
                                && device.getProduct().toLowerCase().contains("interfacekit")));
    }

    public int getSerial() {
        return serial;
    }

    public String getName() {
        return name;
    }

    public int getSensorCount() {
        return sensorCount;
    }

    public boolean open() {
        if (!device.isClosed()) {
            return true;
        }
        return device.open();
    }

    public void close() {
        if (!device.isClosed()) {
            device.close();
        }
    }

    /**
     * Read the current analog sensor values, scaled to the 0..1000 range the
     * rest of RomRaider uses. Returns {@code null} if no report was available.
     */
    public int[] readSensorValues() {
        final byte[] report = new byte[16];
        final int read = device.read(report, 50);
        if (read <= 0) {
            return null;
        }
        final int[] raw = decodeAnalogInputs(report, sensorCount);
        final int[] scaled = new int[raw.length];
        for (int i = 0; i < raw.length; i++) {
            scaled[i] = raw[i] * SENSOR_VALUE_MAX / ADC_MAX;
        }
        return scaled;
    }

    /**
     * Decode packed 12-bit analog inputs from an InterfaceKit HID input report.
     *
     * <p>Per the documented Phidget protocol the analog channels are stored two
     * per three bytes, little-endian within the pair:</p>
     * <pre>
     *   ch0 =  b[0]        + ((b[1] &amp; 0x0F) &lt;&lt; 8)
     *   ch1 = (b[1] &gt;&gt; 4)  +  (b[2] &lt;&lt; 4)
     *   ...
     * </pre>
     *
     * @param report HID input report bytes
     * @param count  number of analog channels to decode
     * @return raw 0..4095 values, one per channel
     */
    static int[] decodeAnalogInputs(byte[] report, int count) {
        final int[] values = new int[count];
        for (int ch = 0; ch < count; ch++) {
            final int pair = ch / 2;
            final int base = pair * 3;
            if (base + 2 >= report.length) {
                break;
            }
            final int b0 = report[base] & 0xFF;
            final int b1 = report[base + 1] & 0xFF;
            final int b2 = report[base + 2] & 0xFF;
            if ((ch & 1) == 0) {
                values[ch] = b0 + ((b1 & 0x0F) << 8);
            } else {
                values[ch] = (b1 >> 4) + (b2 << 4);
            }
            if (values[ch] > ADC_MAX) {
                values[ch] = ADC_MAX;
            }
        }
        return values;
    }

    private static int parseSerial(String serialNumber) {
        if (serialNumber == null) {
            return -1;
        }
        try {
            return Integer.parseInt(serialNumber.trim());
        } catch (NumberFormatException e) {
            // some firmware reports the serial with non-digit decoration
            final StringBuilder digits = new StringBuilder();
            for (int i = 0; i < serialNumber.length(); i++) {
                final char c = serialNumber.charAt(i);
                if (Character.isDigit(c)) {
                    digits.append(c);
                }
            }
            if (digits.length() == 0) {
                LOGGER.warn("Could not parse Phidget serial: " + serialNumber);
                return -1;
            }
            return Integer.parseInt(digits.toString());
        }
    }

    private static final class IkProduct {
        final String name;
        final int analogInputs;

        IkProduct(String name, int analogInputs) {
            this.name = name;
            this.analogInputs = analogInputs;
        }
    }
}
