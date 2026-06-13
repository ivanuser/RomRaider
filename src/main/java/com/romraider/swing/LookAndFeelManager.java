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

package com.romraider.swing;

import static com.romraider.Version.PRODUCT_NAME;
import static com.romraider.util.Platform.MAC_OS_X;
import static com.romraider.util.Platform.isPlatform;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;
import com.romraider.Settings;
import com.romraider.util.SettingsManager;

/**
 * Sets up the modern FlatLaf based user interface, including the available
 * themes, the RomRaider accent styling and animated theme transitions.
 */
public final class LookAndFeelManager {
    private static final Logger LOGGER = Logger.getLogger(LookAndFeelManager.class);

    /** RomRaider signature accent used across all themes. */
    private static final String ACCENT_COLOR = "#00B4FF";

    private static final Map<String, LookAndFeel> THEMES = new LinkedHashMap<String, LookAndFeel>();
    static {
        THEMES.put("Dark", new FlatMacDarkLaf());
        THEMES.put("Light", new FlatMacLightLaf());
        THEMES.put("Carbon", new FlatDarkLaf());
        THEMES.put("Darcula", new FlatDarculaLaf());
        THEMES.put("Arctic", new FlatLightLaf());
        THEMES.put("IntelliJ", new FlatIntelliJLaf());
    }

    private LookAndFeelManager() {
        throw new UnsupportedOperationException();
    }

    /** The names of all available UI themes, in display order. */
    public static Set<String> getThemeNames() {
        return THEMES.keySet();
    }

    public static void initLookAndFeel() {
        try {
            if (isPlatform(MAC_OS_X)) {
                System.setProperty("apple.awt.rendering", "true");
                System.setProperty("apple.awt.brushMetalLook", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.window.position.forceSafeCreation", "true");
                System.setProperty("apple.awt.application.appearance", "system");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", PRODUCT_NAME);
            }

            installGlobalStyle();
            UIManager.setLookAndFeel(themeFor(getSavedThemeName()));

            // make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

        } catch (Exception ex) {
            LOGGER.error("Error loading look and feel.", ex);
        }
    }

    /**
     * Switch the entire application to the given theme with a smooth
     * cross-fade over all open windows, and remember the choice.
     */
    public static void applyTheme(String name) {
        try {
            FlatAnimatedLafChange.showSnapshot();
            UIManager.setLookAndFeel(themeFor(name));
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            SettingsManager.getSettings().setTheme(name);
        } catch (Exception ex) {
            LOGGER.error("Error applying theme: " + name, ex);
        }
    }

    /** The name of the theme that is currently saved in the settings. */
    public static String getSavedThemeName() {
        try {
            final String name = SettingsManager.getSettings().getTheme();
            if (name != null && THEMES.containsKey(name)) return name;
        } catch (Exception ex) {
            LOGGER.error("Error reading theme setting.", ex);
        }
        return Settings.DEFAULT_THEME;
    }

    /**
     * Fade a window in as it opens. The window is made visible by this call.
     * Falls back to a plain setVisible where translucency is unsupported.
     */
    public static void fadeIn(final Window window) {
        final GraphicsDevice device =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean translucency = false;
        try {
            translucency = device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Exception ex) {
            LOGGER.debug("Translucency check failed.", ex);
        }
        if (!translucency) {
            window.setVisible(true);
            return;
        }
        try {
            window.setOpacity(0f);
            window.setVisible(true);
            final Animator animator = new Animator(350, new Animator.TimingTarget() {
                @Override
                public void timingEvent(float fraction) {
                    try {
                        window.setOpacity(fraction);
                    } catch (Exception ex) {
                        // decorated windows may reject opacity changes mid-flight
                    }
                }
                @Override
                public void end() {
                    try {
                        window.setOpacity(1f);
                    } catch (Exception ex) {
                        // ignore, the window is visible either way
                    }
                }
            });
            animator.setInterpolator(CubicBezierEasing.EASE_OUT);
            animator.start();
        } catch (Exception ex) {
            LOGGER.debug("Window fade-in unavailable.", ex);
            try {
                window.setOpacity(1f);
            } catch (Exception ignored) {
            }
            window.setVisible(true);
        }
    }

    private static LookAndFeel themeFor(String name) {
        final LookAndFeel laf = THEMES.get(name);
        return laf != null ? laf : THEMES.get(Settings.DEFAULT_THEME);
    }

    /**
     * The RomRaider 2026 design language: signature accent color, soft
     * rounded corners, pill scroll bars, animated hover/selection feedback
     * and a unified window header with the menu bar embedded in it.
     */
    private static void installGlobalStyle() {
        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");

        FlatLaf.setGlobalExtraDefaults(
                java.util.Collections.singletonMap("@accentColor", ACCENT_COLOR));

        // soft rounded corners everywhere
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("TextComponent.arc", 12);

        // slim animated focus indication
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);

        // pill style scroll bars
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);

        // buttery smooth scrolling
        UIManager.put("ScrollPane.smoothScrolling", true);

        // modern tabs: no clunky borders, animated underline selection
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.selectedBackground", null);
        UIManager.put("TabbedPane.tabSelectionHeight", 3);

        // unified frame header with embedded menu bar
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("TitlePane.menuBarEmbedded", true);

        // roomier menus and tool tips
        UIManager.put("Menu.margin", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("MenuItem.margin", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("MenuItem.selectionType", "underline");
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }
}
