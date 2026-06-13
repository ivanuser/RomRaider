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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * IntfKitConnector opens a connection to each requested InterfaceKit serial
 * number and returns the open devices for the runner to poll.
 */
public final class IntfKitConnector {
    private static final Logger LOGGER = getLogger(IntfKitConnector.class);

    private IntfKitConnector() {
    }

    /**
     * Open a connection to each requested serial number.
     * @param serials list of serial numbers to open
     * @return the set of opened InterfaceKit devices
     * @see IntfKitRunner
     */
    public static Set<PhidgetIkDevice> openIkSerial(final List<Integer> serials) {
        final Set<PhidgetIkDevice> kits = new HashSet<PhidgetIkDevice>();
        for (int serial : serials) {
            final PhidgetIkDevice ik = IntfKitManager.deviceFor(serial);
            if (ik == null) {
                LOGGER.error("InterfaceKit serial " + serial + " not found");
                continue;
            }
            if (ik.open()) {
                kits.add(ik);
            } else {
                LOGGER.error("Unable to open InterfaceKit serial " + serial);
            }
        }
        return kits;
    }
}
