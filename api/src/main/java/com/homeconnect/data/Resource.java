/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.homeconnect.data;

import org.eclipse.jdt.annotation.Nullable;

public enum Resource {

    FREEZER_TEMPERATURE_SETPOINT(Type.SETTINGS, Constants.SETTINGS_FREEZER_SETPOINT_TEMPERATURE),
    FRIDGE_TEMPERATURE_SETPOINT(Type.SETTINGS, Constants.SETTINGS_FRIDGE_SETPOINT_TEMPERATURE),
    FREEZER_SUPER_MODE(Type.SETTINGS, Constants.SETTINGS_FREEZER_SUPER_MODE),
    FRIDGE_SUPER_MODE(Type.SETTINGS, Constants.SETTINGS_FRIDGE_SUPER_MODE);

    public static enum Type {
        SETTINGS,
        STATUS;
    }

    private final Type type;
    private final String key;

    private Resource(Type type, String key) {
        this.type = type;
        this.key = key;
    }

    public Type getType() {
        return this.type;
    }

    public String getKey() {
        return this.key;
    }

    public static @Nullable Resource valueOfKey(String key) {
        for (Resource eventType : Resource.values()) {
            if (eventType.key.equalsIgnoreCase(key)) {
                return eventType;
            }
        }
        return null;
    }

}
