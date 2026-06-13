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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/**
 * A self-contained, native-free 3D surface plot of an ECU table.
 *
 * <p>This is RomRaider's own replacement for the old Java3D based 3D table
 * view.  It renders an interactive, color-mapped surface with software
 * projection and a painter's-algorithm rasterizer using only Java2D, so it
 * has no native libraries and runs on any platform with a JVM.</p>
 *
 * <p>Interaction: drag to rotate, mouse wheel to zoom, click to select a
 * cell, and arrow up/down or +/- to nudge the selected cell's value.  Value
 * and selection changes are reported through {@link GraphDataListener}.</p>
 */
public class Graph3dPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // Surface data: value[z][x]
    private float[][] values;
    private int xWidth;
    private int zDepth;
    private float minValue;
    private float maxValue;
    private String xLabel = "X";
    private String yLabel = "Value";
    private String zLabel = "Z";

    // View state
    private double azimuth = Math.toRadians(35);   // rotation about vertical axis
    private double elevation = Math.toRadians(28);  // tilt
    private double zoom = 1.0;
    private int panX = 0;
    private int panY = 0;

    // Selection
    private int selX = -1;
    private int selZ = -1;

    // Interaction bookkeeping
    private int lastMouseX;
    private int lastMouseY;

    private final List<GraphDataListener> listeners = new ArrayList<GraphDataListener>();

    public Graph3dPanel() {
        setBackground(new Color(18, 20, 26));
        setFocusable(true);
        setPreferredSize(new Dimension(640, 480));
        installInteraction();
    }

    /** Load a surface from the row-major vector the table view produces. */
    public void setData(List<float[]> rows, double min, double max,
            String xLabel, String yLabel, String zLabel) {
        this.zDepth = rows.size();
        this.xWidth = zDepth > 0 ? rows.get(0).length : 0;
        this.values = new float[zDepth][xWidth];
        for (int z = 0; z < zDepth; z++) {
            float[] row = rows.get(z);
            for (int x = 0; x < xWidth && x < row.length; x++) {
                values[z][x] = row[x];
            }
        }
        this.minValue = (float) min;
        this.maxValue = (float) max;
        if (this.maxValue <= this.minValue) {
            this.maxValue = this.minValue + 1f;
        }
        this.xLabel = xLabel != null ? xLabel : "X";
        this.yLabel = yLabel != null ? yLabel : "Value";
        this.zLabel = zLabel != null ? zLabel : "Z";
        this.selX = -1;
        this.selZ = -1;
        repaint();
    }

    public void addGraphDataListener(GraphDataListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeGraphDataListener(GraphDataListener listener) {
        listeners.remove(listener);
    }

    private void installInteraction() {
        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (e.isShiftDown() || e.getButton() == MouseEvent.BUTTON1) {
                    pickCell(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                final int dx = e.getX() - lastMouseX;
                final int dy = e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (e.isControlDown()) {
                    panX += dx;
                    panY += dy;
                } else {
                    azimuth += dx * 0.01;
                    elevation += dy * 0.01;
                    elevation = Math.max(-Math.PI / 2 + 0.05,
                            Math.min(Math.PI / 2 - 0.05, elevation));
                }
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoom *= Math.pow(1.1, -e.getPreciseWheelRotation());
                zoom = Math.max(0.2, Math.min(6.0, zoom));
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (selX < 0 || selZ < 0) {
                    return;
                }
                final double range = maxValue - minValue;
                final float step = (float) (range / 100.0);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_ADD:
                    case KeyEvent.VK_EQUALS:
                        nudgeSelected(step);
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_SUBTRACT:
                    case KeyEvent.VK_MINUS:
                        nudgeSelected(-step);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void nudgeSelected(float delta) {
        values[selZ][selX] += delta;
        fireNewGraphData(selX, selZ, values[selZ][selX]);
        repaint();
    }

    private void fireNewGraphData(int x, int z, float value) {
        for (GraphDataListener l : new ArrayList<GraphDataListener>(listeners)) {
            l.newGraphData(x, z, value);
        }
    }

    private void fireSelectStateChange(int x, int z, boolean selected) {
        for (GraphDataListener l : new ArrayList<GraphDataListener>(listeners)) {
            l.selectStateChange(x, z, selected);
        }
    }

    // --- projection -------------------------------------------------------

    /** Project a normalized model point (mx,my,mz) into screen space. */
    private double[] project(double mx, double my, double mz, int w, int h) {
        // rotate around Y (azimuth)
        final double cosA = Math.cos(azimuth);
        final double sinA = Math.sin(azimuth);
        double rx = mx * cosA - mz * sinA;
        double rz = mx * sinA + mz * cosA;
        // rotate around X (elevation)
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);
        double ry = my * cosE - rz * sinE;
        double depth = my * sinE + rz * cosE;
        // orthographic projection with zoom
        final double scale = Math.min(w, h) * 0.45 * zoom;
        final double sx = w / 2.0 + panX + rx * scale;
        final double sy = h / 2.0 + panY - ry * scale;
        return new double[] { sx, sy, depth };
    }

    private double norm(float value) {
        return (value - minValue) / (maxValue - minValue);
    }

    /** RomRaider-style blue -> green -> yellow -> red height ramp. */
    private Color heatColor(double t) {
        t = Math.max(0, Math.min(1, t));
        final float[] r = { 0.10f, 0.10f, 1.00f, 1.00f };
        final float[] g = { 0.30f, 0.85f, 0.85f, 0.20f };
        final float[] b = { 0.85f, 0.45f, 0.10f, 0.15f };
        final double pos = t * (r.length - 1);
        final int i = (int) Math.floor(pos);
        final int j = Math.min(i + 1, r.length - 1);
        final double f = pos - i;
        return new Color(
                (float) (r[i] + (r[j] - r[i]) * f),
                (float) (g[i] + (g[j] - g[i]) * f),
                (float) (b[i] + (b[j] - b[i]) * f));
    }

    // --- rendering --------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        final int w = getWidth();
        final int h = getHeight();

        if (values == null || xWidth < 2 || zDepth < 2) {
            g2.setColor(Color.GRAY);
            g2.drawString("No 3D data", w / 2 - 30, h / 2);
            g2.dispose();
            return;
        }

        // Build quads and sort back-to-front (painter's algorithm).
        final List<Quad> quads = new ArrayList<Quad>((xWidth - 1) * (zDepth - 1));
        for (int z = 0; z < zDepth - 1; z++) {
            for (int x = 0; x < xWidth - 1; x++) {
                final double[] p0 = vertex(x, z, w, h);
                final double[] p1 = vertex(x + 1, z, w, h);
                final double[] p2 = vertex(x + 1, z + 1, w, h);
                final double[] p3 = vertex(x, z + 1, w, h);
                final double avg = (norm(values[z][x]) + norm(values[z][x + 1])
                        + norm(values[z + 1][x + 1]) + norm(values[z + 1][x])) / 4.0;
                final double depth = (p0[2] + p1[2] + p2[2] + p3[2]) / 4.0;
                quads.add(new Quad(x, z, p0, p1, p2, p3, avg, depth));
            }
        }
        quads.sort((a, b) -> Double.compare(a.depth, b.depth));

        final BasicStroke edge = new BasicStroke(0.6f);
        g2.setStroke(edge);
        for (Quad q : quads) {
            final Path2D.Double path = new Path2D.Double();
            path.moveTo(q.p0[0], q.p0[1]);
            path.lineTo(q.p1[0], q.p1[1]);
            path.lineTo(q.p2[0], q.p2[1]);
            path.lineTo(q.p3[0], q.p3[1]);
            path.closePath();

            final boolean selected = (q.x == selX && q.z == selZ);
            Color fill = heatColor(q.avg);
            if (selected) {
                fill = fill.brighter().brighter();
            }
            g2.setColor(fill);
            g2.fill(path);
            g2.setColor(selected ? Color.WHITE : new Color(0, 0, 0, 90));
            g2.setStroke(selected ? new BasicStroke(2.2f) : edge);
            g2.draw(path);
        }

        drawAxes(g2, w, h);
        g2.dispose();
    }

    private double[] vertex(int x, int z, int w, int h) {
        // map grid to model coordinates in [-1,1] with height in [0,1]
        final double mx = (x / (double) (xWidth - 1)) * 2 - 1;
        final double mz = (z / (double) (zDepth - 1)) * 2 - 1;
        final double my = norm(values[z][x]); // 0..1 height
        return project(mx, my * 1.1, mz, w, h);
    }

    private void drawAxes(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(170, 180, 200));
        final double[] origin = project(-1, 0, -1, w, h);
        final double[] xEnd = project(1, 0, -1, w, h);
        final double[] zEnd = project(-1, 0, 1, w, h);
        final double[] yEnd = project(-1, 1.1, -1, w, h);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine((int) origin[0], (int) origin[1], (int) xEnd[0], (int) xEnd[1]);
        g2.drawLine((int) origin[0], (int) origin[1], (int) zEnd[0], (int) zEnd[1]);
        g2.drawLine((int) origin[0], (int) origin[1], (int) yEnd[0], (int) yEnd[1]);
        g2.drawString(xLabel, (int) xEnd[0], (int) xEnd[1]);
        g2.drawString(zLabel, (int) zEnd[0], (int) zEnd[1]);
        g2.drawString(yLabel, (int) yEnd[0] - 10, (int) yEnd[1] - 6);
    }

    // --- picking ----------------------------------------------------------

    private void pickCell(int mouseX, int mouseY) {
        if (values == null) {
            return;
        }
        final int w = getWidth();
        final int h = getHeight();
        double best = Double.MAX_VALUE;
        int bx = -1;
        int bz = -1;
        for (int z = 0; z < zDepth; z++) {
            for (int x = 0; x < xWidth; x++) {
                final double[] p = vertex(x, z, w, h);
                final double d = (p[0] - mouseX) * (p[0] - mouseX)
                        + (p[1] - mouseY) * (p[1] - mouseY);
                if (d < best) {
                    best = d;
                    bx = x;
                    bz = z;
                }
            }
        }
        if (bx >= 0 && best < 900) { // within ~30px
            if (selX >= 0) {
                fireSelectStateChange(selX, selZ, false);
            }
            selX = bx;
            selZ = bz;
            fireSelectStateChange(selX, selZ, true);
            repaint();
        }
    }

    private static final class Quad {
        final int x;
        final int z;
        final double[] p0;
        final double[] p1;
        final double[] p2;
        final double[] p3;
        final double avg;
        final double depth;

        Quad(int x, int z, double[] p0, double[] p1, double[] p2, double[] p3,
                double avg, double depth) {
            this.x = x;
            this.z = z;
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.avg = avg;
            this.depth = depth;
        }
    }
}
