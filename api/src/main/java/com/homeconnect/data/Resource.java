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
 
	FREEZER_SETPOINT_TEMPERATURE(Type.EVENT, Constants.SETTINGS_FREEZER_SETPOINT_TEMPERATURE,
    		HomeConnectApiClient.VALUE_TYPE_INT),
    FRIDGE_SETPOINT_TEMPERATURE(Type.EVENT, Constants.SETTINGS_FRIDGE_SETPOINT_TEMPERATURE, 
    		HomeConnectApiClient.VALUE_TYPE_INT),
    FREEZER_SUPER_MODE(Type.EVENT, Constants.SETTINGS_FREEZER_SUPER_MODE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    FRIDGE_SUPER_MODE(Type.EVENT, Constants.SETTINGS_FRIDGE_SUPER_MODE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    //FRIDGE_ECO_MODE(Type.SETTINGS, Constants.SETTINGS_FRIDGE_ECO_MODE, 
    //		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
    FRIDGE_MEASURED_TEMPERATURE(Type.EVENT, Constants.EVENT_FRIDGE_MEASURED_TEMPERATURE, 
    	    		HomeConnectApiClient.VALUE_TYPE_DOUBLE),
    FREEZER_MEASURED_TEMPERATURE(Type.EVENT, Constants.EVENT_FREEZER_MEASURED_TEMPERATURE, 
    		HomeConnectApiClient.VALUE_TYPE_DOUBLE),
	
    WASHER_ACTIVE_PROGRAM(Type.EVENT, Constants.EVENT_ACTIVE, 
    		HomeConnectApiClient.VALUE_TYPE_STRING),
    WASHER_OPERATION_STATE(Type.EVENT, Constants.EVENT_STATUS_OPERATION_STATE, 
    		HomeConnectApiClient.VALUE_TYPE_STRING),
    WASHER_PROGRAM_PROGRESS(Type.EVENT, Constants.OPTION_TOTAL_PROGRAM_PROGRESS, 
    		HomeConnectApiClient.VALUE_TYPE_INT),
	WASHER_SELECTED_PROGRAM(Type.EVENT, Constants.EVENT_PROGRAM, 
			HomeConnectApiClient.VALUE_TYPE_STRING),
	WASHER_TIME_LEFT(Type.EVENT, Constants.OPTION_ESTIMATED_TOTAL_PROGRAM_TIME, 
			HomeConnectApiClient.VALUE_TYPE_INT),
	WASHER_END_TIME(Type.EVENT, Constants.EVENT_OPTION_FINISH_IN_RELATIVE, 
			HomeConnectApiClient.VALUE_TYPE_LONG),
	
	DISHWASHER_ACTIVE_PROGRAM(Type.PROGRAM_ACTIVE, Constants.NONE, 
    		HomeConnectApiClient.VALUE_TYPE_BOOLEAN),
	DISHWASHER_PROGRAM_PROGRESS(Type.PROGRAM_ACTIVE_OPTIONS, Constants.OPTION_TOTAL_PROGRAM_PROGRESS, 
    		HomeConnectApiClient.VALUE_TYPE_INT),
	DISHWASHER_SELECTED_PROGRAM(Type.PROGRAM_SELECTED, Constants.NONE, 
			HomeConnectApiClient.VALUE_TYPE_STRING),
	DISHWASHER_TIME_LEFT(Type.PROGRAM_ACTIVE_OPTIONS, Constants.OPTION_ESTIMATED_TOTAL_PROGRAM_TIME, 
			HomeConnectApiClient.VALUE_TYPE_INT),
	DISHWASHER_START_PROGRAM_TIME(Type.PROGRAM_ACTIVE_OPTIONS, Constants.OPTION_START_IN_RELATIVE, 
			HomeConnectApiClient.VALUE_TYPE_LONG);
	

    public static enum Type {
        SETTINGS,
        STATUS,
    	PROGRAM_AVAILABLE,
    	PROGRAM_ACTIVE,
    	PROGRAM_SELECTED,
    	PROGRAM_ACTIVE_OPTIONS,
    	EVENT;
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
    
    public String getValueTypeAsString() {
    	String valueType = "String";
		switch(this.getValueType()) {
		case 0: valueType = "String";
		case 1: valueType = "Integer";
		case 2: valueType = "Boolean";
		case 3: valueType = "Double";
		case 4: valueType = "Long";
		}
		return valueType;
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
