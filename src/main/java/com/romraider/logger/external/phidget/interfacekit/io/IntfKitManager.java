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

import static org.apache.log4j.Logger.getLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import com.romraider.util.SettingsManager;

/**
 * IntfKitManager discovers all attached PhidgetInterfaceKits by serial number
 * and builds the list of sensors on each one.
 *
 * <p>This is the native-free reimplementation that talks to the InterfaceKits
 * directly over USB HID via {@link PhidgetIkDevice}, replacing the end-of-life
 * phidget21 JNI wrapper.</p>
 */
public final class IntfKitManager {
    private static final Logger LOGGER = getLogger(IntfKitManager.class);

    private IntfKitManager() {
    }

    /** Shared HID services handle, lazily started. */
    private static HidServices hidServices() {
        return HidManager.getHidServices();
    }

    /**
     * Find all attached PhidgetInterfaceKits.
     * @return a list of serial numbers
     */
    public static List<Integer> findIntfkits() {
        final List<Integer> serials = new ArrayList<Integer>();
        try {
            for (HidDevice device : hidServices().getAttachedHidDevices()) {
                if (PhidgetIkDevice.isInterfaceKit(device)) {
                    final PhidgetIkDevice ik = new PhidgetIkDevice(device);
                    if (ik.getSerial() != -1 && !serials.contains(ik.getSerial())) {
                        serials.add(ik.getSerial());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Phidget HID discovery error: " + e);
        }
        return serials;
    }

    /**
     * Initialise the HID services and report the library version to the log.
     */
    public static void loadIk() {
        try {
            LOGGER.info("Phidget plugin using hid4java " + HidServices.getVersion()
                    + " (hidapi " + HidServices.getNativeVersion() + ")");
        } catch (Exception e) {
            LOGGER.error("Phidget HID init error: " + e);
        }
    }

    /**
     * Report the name of the InterfaceKit with the given serial number.
     * @param serial the serial number to look up
     * @return a name/serial description, or {@code null} if not found
     */
    public static String getIkName(final int serial) {
        final PhidgetIkDevice ik = deviceFor(serial);
        if (ik == null) {
            return null;
        }
        return String.format("%s serial: %d", ik.getName(), serial);
    }

    /**
     * Build the set of sensors found on the InterfaceKit with the given serial.
     * @param serial the serial number to interrogate
     * @return a set of {@link IntfKitSensor}
     */
    public static Set<IntfKitSensor> getSensors(final int serial) {
        final Set<IntfKitSensor> sensors = new HashSet<IntfKitSensor>();
        final PhidgetIkDevice ik = deviceFor(serial);
        if (ik == null) {
            LOGGER.info("No InterfaceKit found for serial " + serial);
            return sensors;
        }
        LOGGER.info(String.format("Plugin found: %s Serial: %d", ik.getName(), serial));

        Map<String, IntfKitSensor> stored = SettingsManager.getSettings().getPhidgetSensors();
        if (stored == null) {
            stored = new HashMap<String, IntfKitSensor>();
        }
        final int inputCount = ik.getSensorCount();
        for (int i = 0; i < inputCount; i++) {
            final String key = String.format("%d:%d", serial, i);
            if (stored.containsKey(key)) {
                sensors.add(stored.get(key));
                LOGGER.info("Plugin applying user settings for: " + stored.get(key));
            } else {
                final IntfKitSensor sensor = new IntfKitSensor();
                sensor.setInputNumber(i);
                sensor.setInputName(String.format("Phidget IK Sensor %d:%d", serial, i));
                sensor.setUnits("raw value");
                sensor.setExpression("x");
                sensor.setFormat("0");
                sensor.setMinValue(0);
                sensor.setMaxValue(1000);
                sensor.setStepValue(100);
                sensors.add(sensor);
                stored.put(key, sensor);
            }
        }
        SettingsManager.getSettings().setPhidgetSensors(stored);
        return sensors;
    }

    /** Find the attached InterfaceKit matching a serial number, or null. */
    static PhidgetIkDevice deviceFor(final int serial) {
        try {
            for (HidDevice device : hidServices().getAttachedHidDevices()) {
                if (PhidgetIkDevice.isInterfaceKit(device)) {
                    final PhidgetIkDevice ik = new PhidgetIkDevice(device);
                    if (ik.getSerial() == serial) {
                        return ik;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Phidget HID lookup error: " + e);
        }
        return null;
    }
}
