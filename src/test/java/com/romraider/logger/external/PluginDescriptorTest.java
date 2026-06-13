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

package com.romraider.logger.external;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

import com.romraider.logger.external.core.ExternalDataSource;

/**
 * Verifies that every plugin descriptor in the {@code plugins} directory names
 * a class that exists, is loadable, and implements {@link ExternalDataSource}.
 *
 * <p>This guards against a plugin descriptor drifting out of sync with the code
 * (renamed/moved/removed data source class) without instantiating any plugin,
 * so it needs no hardware and is safe to run in CI.</p>
 */
public class PluginDescriptorTest {

    private static final String DATASOURCE_CLASS = "datasource.class";

    @Test
    public void everyPluginDescriptorResolvesToAnExternalDataSource() throws Exception {
        final File pluginsDir = findPluginsDir();
        assertNotNull("Could not locate the plugins directory", pluginsDir);

        final File[] descriptors = pluginsDir.listFiles((dir, name) -> name.endsWith(".plugin"));
        assertNotNull("No plugin descriptors found", descriptors);
        assertTrue("Expected at least one plugin descriptor", descriptors.length > 0);

        for (File descriptor : descriptors) {
            final Properties props = new Properties();
            try (InputStream in = new FileInputStream(descriptor)) {
                props.load(in);
            }
            final String className = props.getProperty(DATASOURCE_CLASS);
            assertFalse(descriptor.getName() + " has no " + DATASOURCE_CLASS,
                    className == null || className.trim().isEmpty());

            final Class<?> clazz;
            try {
                clazz = Class.forName(className.trim());
            } catch (ClassNotFoundException e) {
                fail(descriptor.getName() + " references missing class: " + className);
                return;
            }
            assertTrue(className + " (" + descriptor.getName()
                            + ") must implement ExternalDataSource",
                    ExternalDataSource.class.isAssignableFrom(clazz));
        }
    }

    /** Locate the plugins directory relative to the working directory or module. */
    private static File findPluginsDir() {
        final String[] candidates = { "plugins", "../plugins", "./plugins" };
        for (String candidate : candidates) {
            final File dir = new File(candidate);
            if (dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }
}
