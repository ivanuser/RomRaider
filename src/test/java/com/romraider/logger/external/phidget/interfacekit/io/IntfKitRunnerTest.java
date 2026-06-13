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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.romraider.logger.external.phidget.interfacekit.plugin.IntfKitDataItem;

public class IntfKitRunnerTest {

    @Test
    public void updatesMatchingDataItemAndTracksLastValue() throws Exception {
        final IntfKitDataItem dataItem = new IntfKitDataItem(newSensor("Phidget IK Sensor 123:2"));
        final Map<String, IntfKitDataItem> items = new HashMap<String, IntfKitDataItem>();
        items.put("123:2", dataItem);
        final IntfKitRunner runner = new IntfKitRunner(Collections.<Integer>emptyList(), items);

        runner.updateDataItem(123, 2, 345);
        assertEquals(345.0, dataItem.getData(), 0.0);

        @SuppressWarnings("unchecked")
        final Map<String, Integer> lastValues =
                (Map<String, Integer>) field(runner, "lastValues").get(runner);
        assertEquals(Integer.valueOf(345), lastValues.get("123:2"));

        runner.updateDataItem(123, 2, 345);
        assertEquals(1, lastValues.size());

        runner.updateDataItem(123, 2, 500);
        assertEquals(500.0, dataItem.getData(), 0.0);
        assertEquals(Integer.valueOf(500), lastValues.get("123:2"));
    }

    @Test
    public void ignoresInvalidSerialAndDoesNotTrackValue() throws Exception {
        final IntfKitRunner runner = new IntfKitRunner(
                Collections.<Integer>emptyList(),
                Collections.<String, IntfKitDataItem>emptyMap());

        runner.updateDataItem(-1, 0, 123);

        @SuppressWarnings("unchecked")
        final Map<String, Integer> lastValues =
                (Map<String, Integer>) field(runner, "lastValues").get(runner);
        assertTrue(lastValues.isEmpty());
    }

    @Test
    public void tracksValueWithoutDataItemWhenKeyMissing() throws Exception {
        final IntfKitRunner runner = new IntfKitRunner(
                Collections.<Integer>emptyList(),
                Collections.<String, IntfKitDataItem>emptyMap());

        runner.updateDataItem(777, 1, 50);

        @SuppressWarnings("unchecked")
        final Map<String, Integer> lastValues =
                (Map<String, Integer>) field(runner, "lastValues").get(runner);
        assertEquals(Integer.valueOf(50), lastValues.get("777:1"));
    }

    private static Field field(Object target, String name) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static IntfKitSensor newSensor(String name) {
        final IntfKitSensor sensor = new IntfKitSensor();
        sensor.setInputName(name);
        sensor.setExpression("x");
        sensor.setUnits("raw value");
        sensor.setFormat("0");
        sensor.setMinValue(0);
        sensor.setMaxValue(1000);
        sensor.setStepValue(100);
        return sensor;
    }
}
