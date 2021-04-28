/*
 * Copyright 2016-20 ISC Konstanz
 *
 * This file is part of OpenHomeConnect.
 * For more information visit https://github.com/isc-konstanz/OpenHomeConnect.
 *
 * OpenHomeConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenHomeConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeConnect.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.homeconnect.data;

import org.eclipse.jdt.annotation.Nullable;
import com.homeconnect.client.HomeConnectApiClient;

public enum Resource {

    FREEZER_TEMPERATURE_SETPOINT(Type.SETTINGS, Constants.SETTINGS_FREEZER_SETPOINT_TEMPERATURE,
    		HomeConnectApiClient.VALUE_TYPE_INT),
    FRIDGE_TEMPERATURE_SETPOINT(Type.SETTINGS, Constants.SETTINGS_FRIDGE_SETPOINT_TEMPERATURE, 
    		HomeConnectApiClient.VALUE_TYPE_INT),
    FREEZER_SUPER_MODE(Type.SETTINGS, Constants.SETTINGS_FREEZER_SUPER_MODE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    FRIDGE_SUPER_MODE(Type.SETTINGS, Constants.SETTINGS_FRIDGE_SUPER_MODE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    FRIDGE_ECO_MODE(Type.SETTINGS, Constants.SETTINGS_FRIDGE_ECO_MODE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    
    WASHER_ACTIVE_PROGRAM(Type.PROGRAM_ACTIVE, Constants.DUMMY, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
	WASHER_SELECTED_PROGRAM(Type.PROGRAM_SELECTED, Constants.DUMMY, 
			HomeConnectApiClient.VALUE_TYPE_STRING),
	WASHER_TIME_LEFT(Type.PROGRAM_ACTIVE_OPTIONS, Constants.OPTION_ESTIMATED_TOTAL_PROGRAM_TIME, 
			HomeConnectApiClient.VALUE_TYPE_INT),
	WASHER_END_TIME(Type.PROGRAM_ACTIVE_OPTIONS, Constants.OPTION_FINISH_IN_RELATIVE, 
			HomeConnectApiClient.VALUE_TYPE_INT);

    public static enum Type {
        SETTINGS,
        STATUS,
    	PROGRAM_AVAILABLE,
    	PROGRAM_ACTIVE,
    	PROGRAM_SELECTED,
    	PROGRAM_ACTIVE_OPTIONS;
    }

    private final Type type;
    private final String key;
    private final int valueType;

    private Resource(Type type, String key, int valueType) {
        this.type = type;
        this.key = key;
        this.valueType = valueType;
    }

    public Type getType() {
        return this.type;
    }

    public String getKey() {
        return this.key;
    }

    public int getValueType() {
    	return this.valueType;
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
