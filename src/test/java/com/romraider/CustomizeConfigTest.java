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

package com.romraider;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.Test;

/**
 * Guards the user-customizable runtime configuration in the {@code customize}
 * directory: it confirms each properties file parses and still carries the
 * keys the code reads, and that the warning sound is a loadable audio clip.
 *
 * <p>This keeps the config files from silently drifting out of sync with the
 * code that consumes them (J2534LibraryLocator, DefinitionManager, the SSM/NCS
 * learning-table matchers, and the dash warning sound).</p>
 */
public class CustomizeConfigTest {

    private final File customizeDir = findCustomizeDir();

    @Test
    public void j2534LibrariesHasPlatformKeys() throws Exception {
        final Properties p = load("j2534Libraries.properties");
        // J2534LibraryLocator reads these two keys by name.
        assertTrue("missing 'windows' key", p.containsKey("windows"));
        assertTrue("missing 'linux' key", p.containsKey("linux"));
    }

    @Test
    public void ssmLearningHasMatcherKeys() throws Exception {
        final Properties p = load("ssmlearning.properties");
        assertTrue(p.containsKey("af_table_names"));
        assertTrue(p.containsKey("flkc_table_column_names"));
        assertTrue(p.containsKey("flkc_table_row_names"));
    }

    @Test
    public void ncsLearningHasMatcherKeys() throws Exception {
        final Properties p = load("ncslearning.properties");
        assertTrue(p.containsKey("ltft_table_column_names"));
        assertTrue(p.containsKey("ltft_table_row_names"));
    }

    @Test
    public void nameSequencesIsNonEmpty() throws Exception {
        final Properties p = load("nameSequences.properties");
        // DefinitionManager matches a file's content against each value here.
        assertTrue("nameSequences should define at least one mapping", !p.isEmpty());
    }

    @Test
    public void warningSoundIsLoadableAudio() throws Exception {
        final File wav = new File(customizeDir, "warningSound.wav");
        assertTrue("warningSound.wav is missing", wav.isFile());
        final AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
        try {
            assertTrue("warningSound.wav has no audio frames", ais.getFrameLength() > 0);
        } finally {
            ais.close();
        }
    }

    private Properties load(String name) throws Exception {
        final Properties props = new Properties();
        try (InputStream in = new FileInputStream(new File(customizeDir, name))) {
            props.load(in);
        }
        return props;
    }

    private static File findCustomizeDir() {
        final String[] candidates = { "customize", "../customize", "./customize" };
        for (String candidate : candidates) {
            final File dir = new File(candidate);
            if (dir.isDirectory()) {
                return dir;
            }
        }
        throw new IllegalStateException("Could not locate the customize directory");
    }
}
