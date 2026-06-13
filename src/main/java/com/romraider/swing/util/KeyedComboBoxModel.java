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

package com.romraider.swing.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A combo box model that pairs each visible item with a hidden key object.
 * Replacement for the retired JCommon class of the same name.
 */
public class KeyedComboBoxModel implements ComboBoxModel {
    private final Object[] keys;
    private final Object[] values;
    private final List<ListDataListener> listeners = new ArrayList<ListDataListener>();
    private int selectedIndex = -1;

    /**
     * @param keys   the hidden key for each entry
     * @param values the visible item for each entry
     */
    public KeyedComboBoxModel(final Object[] keys, final Object[] values) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Keys and values must have the same length.");
        }
        this.keys = keys.clone();
        this.values = values.clone();
    }

    public Object getSelectedKey() {
        return selectedIndex == -1 ? null : keys[selectedIndex];
    }

    public void setSelectedKey(final Object key) {
        selectedIndex = indexOf(keys, key);
        fireSelectionChanged();
    }

    @Override
    public Object getSelectedItem() {
        return selectedIndex == -1 ? null : values[selectedIndex];
    }

    @Override
    public void setSelectedItem(final Object item) {
        selectedIndex = indexOf(values, item);
        fireSelectionChanged();
    }

    @Override
    public Object getElementAt(final int index) {
        return values[index];
    }

    @Override
    public int getSize() {
        return values.length;
    }

    @Override
    public void addListDataListener(final ListDataListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListDataListener(final ListDataListener listener) {
        listeners.remove(listener);
    }

    private static int indexOf(final Object[] array, final Object object) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null ? object == null : array[i].equals(object)) {
                return i;
            }
        }
        return -1;
    }

    private void fireSelectionChanged() {
        final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        for (ListDataListener listener : new ArrayList<ListDataListener>(listeners)) {
            listener.contentsChanged(event);
        }
    }
}
