/* 
 * Copyright 2020-2022 ISC Konstanz
 * 
 * This file is part of OpenHomeConnect.
 * For more information visit https://github.com/isc-konstanz/OpenHomeConnect
 * 
 * OpenHomeConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenHomeConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenHomeConnect.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmuc.framework.driver.homeconnect;

import static org.openmuc.framework.config.option.annotation.OptionType.ADDRESS;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.option.annotation.Option;
import org.openmuc.framework.config.option.annotation.Syntax;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.DriverChannel;
import org.openmuc.framework.driver.annotation.Configure;

import com.homeconnect.client.model.Data;
import com.homeconnect.data.Resource;

@Syntax(separator = "@")
public class HomeConnectChannel extends DriverChannel {

    private Resource resource;

    @Option(type = ADDRESS,
    		id = "resource",
            name = "Resource",
            description = "The resource of the home appliance, referenced by this channel",
            mandatory = true)
    private String resourceString;

    @Option(type = ADDRESS,
            name = "Home appliance ID",
            description = "The unique home appliance ID",
            mandatory = true)
    private String haId;

    private Value value = null; 

    @Configure
    public void setResource() throws ArgumentSyntaxException {
        try {
            resource = Resource.valueOf(resourceString.replace("-", "_"));
            
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException("Unknown resource: " + resourceString);
        }
    }

    public Resource getResource() {
        return resource;
    }

    public String getHomeApplianceId() {
        return haId; 
    }

    public void setData(Data data, long timestamp) {
    	
    	switch(getValueType()) {
		case BOOLEAN:
			value = new BooleanValue(data.getValueAsBoolean());
			break;
		case BYTE:
			value = new ByteValue(data.getValueAsByte());
			break;
		case BYTE_ARRAY:
			value = new ByteArrayValue(data.getValueAsByteArray());
			break;
		case DOUBLE:
			value = new DoubleValue(data.getValueAsDouble());
			break;
		case FLOAT:
			value = new FloatValue(data.getValueAsFloat());
			break;
		case INTEGER:
			value = new IntValue(data.getValueAsInt());
			break;
		case LONG:
			value = new LongValue(data.getValueAsLong());
			break;
		case SHORT:
			value = new ShortValue(data.getValueAsShort());
			break;
		case STRING:
			value = new StringValue(data.getValue());
			break;
		default:
			break;
    	
    	}
        setRecord(new Record(value,timestamp,Flag.VALID));
    }   
}
