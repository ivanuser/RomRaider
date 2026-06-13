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

/**
 * Receives notifications when the 3D surface editor changes a cell value or
 * its selection state, so the owning table view can stay in sync.
 *
 * <p>This is the modern, dependency-free replacement for the listener that
 * used to live in the prebuilt Graph3d/Java3D library.</p>
 */
public interface GraphDataListener {

    /**
     * A cell's value was changed in the 3D editor.
     *
     * @param x     column index into the surface
     * @param z     row index into the surface
     * @param value the new real value
     */
    void newGraphData(int x, int z, float value);

    /**
     * A cell's selection state changed in the 3D editor.
     *
     * @param x        column index into the surface
     * @param z        row index into the surface
     * @param selected whether the cell is now selected
     */
    void selectStateChange(int x, int z, boolean selected);
}
