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

package com.romraider.swing.graph3d;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class Graph3dPanelTest {

    @Test
    public void setDataCopiesValuesAndAppliesDefaults() throws Exception {
        final Graph3dPanel panel = new Graph3dPanel();
        panel.setSize(640, 480);
        final float[] row0 = new float[] { 1f, 2f, 3f };
        final List<float[]> rows = Arrays.asList(row0, new float[] { 4f, 5f });

        panel.setData(rows, 10, 10, null, null, null);

        assertEquals(2, intField(panel, "zDepth"));
        assertEquals(3, intField(panel, "xWidth"));
        assertEquals(10f, floatField(panel, "minValue"), 0f);
        assertEquals(11f, floatField(panel, "maxValue"), 0f);
        assertEquals("X", stringField(panel, "xLabel"));
        assertEquals("Value", stringField(panel, "yLabel"));
        assertEquals("Z", stringField(panel, "zLabel"));
        assertEquals(-1, intField(panel, "selX"));
        assertEquals(-1, intField(panel, "selZ"));

        final float[][] copied = (float[][]) field(panel, "values").get(panel);
        row0[1] = 99f;
        assertEquals(2f, copied[0][1], 0f);
    }

    @Test
    public void clickSelectAndArrowKeysPublishEdits() throws Exception {
        final Graph3dPanel panel = new Graph3dPanel();
        panel.setSize(640, 480);
        panel.setData(
                Arrays.asList(
                        new float[] { 0f, 10f, 20f },
                        new float[] { 30f, 40f, 50f },
                        new float[] { 60f, 70f, 80f }),
                0, 100, "Load", "Fuel", "RPM");
        final RecordingListener listener = new RecordingListener();
        panel.addGraphDataListener(listener);

        final double[] p = (double[]) method(panel, "vertex", int.class, int.class, int.class, int.class)
                .invoke(panel, 1, 1, panel.getWidth(), panel.getHeight());
        final int clickX = (int) Math.round(p[0]);
        final int clickY = (int) Math.round(p[1]);
        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                clickX,
                clickY,
                1,
                false,
                MouseEvent.BUTTON1));

        assertEquals(1, intField(panel, "selX"));
        assertEquals(1, intField(panel, "selZ"));
        assertEquals(1, listener.selectionOnCount);

        final KeyEvent up = new KeyEvent(
                panel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_UP,
                KeyEvent.CHAR_UNDEFINED);
        final KeyEvent down = new KeyEvent(
                panel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_DOWN,
                KeyEvent.CHAR_UNDEFINED);
        Arrays.stream(panel.getKeyListeners()).forEach(keyListener -> {
            keyListener.keyPressed(up);
            keyListener.keyPressed(down);
        });

        final float[][] values = (float[][]) field(panel, "values").get(panel);
        assertEquals(40f, values[1][1], 0.0001f);
        assertTrue(listener.newDataCount >= 2);
    }

    @Test
    public void dragAndWheelUpdateViewStateWithClamps() throws Exception {
        final Graph3dPanel panel = new Graph3dPanel();
        panel.setSize(640, 480);
        panel.setData(
                Arrays.asList(
                        new float[] { 0f, 5f },
                        new float[] { 10f, 20f }),
                0, 20, "X", "Y", "Z");

        final double azimuthBefore = doubleField(panel, "azimuth");
        final double elevationBefore = doubleField(panel, "elevation");
        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                100,
                100,
                1,
                false,
                MouseEvent.BUTTON1));
        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                0,
                130,
                140,
                0,
                false,
                MouseEvent.BUTTON1));

        assertTrue(doubleField(panel, "azimuth") != azimuthBefore);
        assertTrue(doubleField(panel, "elevation") != elevationBefore);

        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                InputEvent.CTRL_DOWN_MASK,
                130,
                140,
                1,
                false,
                MouseEvent.BUTTON1));
        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                InputEvent.CTRL_DOWN_MASK,
                150,
                175,
                0,
                false,
                MouseEvent.BUTTON1));
        assertTrue(intField(panel, "panX") != 0);
        assertTrue(intField(panel, "panY") != 0);

        panel.dispatchEvent(new MouseWheelEvent(
                panel,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                100,
                100,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                200));
        assertEquals(0.2, doubleField(panel, "zoom"), 0.0001);

        panel.dispatchEvent(new MouseWheelEvent(
                panel,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                100,
                100,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                -200));
        assertEquals(6.0, doubleField(panel, "zoom"), 0.0001);
    }

    @Test
    public void clickTooFarFromSurfaceDoesNotSelect() throws Exception {
        final Graph3dPanel panel = new Graph3dPanel();
        panel.setSize(640, 480);
        panel.setData(
                Arrays.asList(
                        new float[] { 0f, 1f },
                        new float[] { 2f, 3f }),
                0, 3, "X", "Y", "Z");
        final RecordingListener listener = new RecordingListener();
        panel.addGraphDataListener(listener);

        panel.dispatchEvent(new MouseEvent(
                panel,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                5,
                5,
                1,
                false,
                MouseEvent.BUTTON1));

        assertEquals(-1, intField(panel, "selX"));
        assertEquals(-1, intField(panel, "selZ"));
        assertEquals(0, listener.selectionOnCount);
    }

    private static Method method(Object target, String name, Class<?>... args) throws Exception {
        final Method m = target.getClass().getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    private static Field field(Object target, String name) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static int intField(Object target, String name) throws Exception {
        return ((Integer) field(target, name).get(target)).intValue();
    }

    private static float floatField(Object target, String name) throws Exception {
        return ((Float) field(target, name).get(target)).floatValue();
    }

    private static double doubleField(Object target, String name) throws Exception {
        return ((Double) field(target, name).get(target)).doubleValue();
    }

    private static String stringField(Object target, String name) throws Exception {
        return (String) field(target, name).get(target);
    }

    private static final class RecordingListener implements GraphDataListener {
        int newDataCount;
        int selectionOnCount;

        @Override
        public void newGraphData(int x, int z, float value) {
            newDataCount++;
        }

        @Override
        public void selectStateChange(int x, int z, boolean selected) {
            if (selected) {
                selectionOnCount++;
            }
        }
    }
}
