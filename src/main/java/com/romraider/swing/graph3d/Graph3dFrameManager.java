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

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Owns the single 3D surface window for the editor.  Drop-in replacement for
 * the old Java3D based frame manager, built entirely on {@link Graph3dPanel}
 * so it carries no native dependencies.
 */
public final class Graph3dFrameManager {

    private static JFrame frame;
    private static Graph3dPanel panel;

    private Graph3dFrameManager() {
    }

    /**
     * Open (or refresh) the 3D surface window for a table.
     *
     * @param values  row-major surface values, one {@code float[]} per row
     * @param min     minimum real value (height scale floor)
     * @param max     maximum real value (height scale ceiling)
     * @param xValues x-axis values (used for the axis label only)
     * @param zValues z-axis values (used for the axis label only)
     * @param xLabel  x-axis label
     * @param yLabel  value-axis label
     * @param zLabel  z-axis label
     * @param title   window title
     * @return the live {@link Graph3dPanel} so the caller can attach a listener
     */
    public static synchronized Graph3dPanel openGraph3dFrame(
            List<float[]> values, double min, double max,
            double[] xValues, double[] zValues,
            String xLabel, String yLabel, String zLabel, String title) {

        if (frame == null) {
            frame = new JFrame();
            panel = new Graph3dPanel();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            final JLabel hint = new JLabel(
                    "Drag to rotate • wheel to zoom • Ctrl+drag to pan "
                    + "• click a cell • ↑/↓ to adjust",
                    SwingConstants.CENTER);
            hint.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
            frame.add(hint, BorderLayout.SOUTH);
            frame.setSize(720, 560);
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            frame.setLocationByPlatform(true);
        }

        panel.setData(values, min, max, xLabel, yLabel, zLabel);
        frame.setTitle(title == null ? "3D View" : title + " - 3D View");
        frame.setVisible(true);
        frame.toFront();
        panel.requestFocusInWindow();
        return panel;
    }

    /** Close the 3D surface window if it is open. */
    public static synchronized void closeGraph3dFrame() {
        if (frame != null) {
            frame.setVisible(false);
        }
    }

    /** The live panel, or {@code null} if the window has never been opened. */
    public static synchronized Graph3dPanel getPanel() {
        return panel;
    }
}
