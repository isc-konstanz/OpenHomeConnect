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
package org.openmuc.framework.driver.homeconnect;

import static com.homeconnect.data.Constants.API_BASE_URL;

import java.text.MessageFormat;
import java.util.List;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.Device;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.options.Address;
import org.openmuc.framework.options.AddressSyntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.homeconnect.client.HomeConnectApiClient;
import com.homeconnect.client.exception.HomeConnectException;

@AddressSyntax(separator = "@")
public class HomeConnection extends Device<HomeConnectChannel> {

    private static final Logger logger = LoggerFactory.getLogger(HomeConnection.class);

    @Address(id = "username",
             name = "Username",
             description = "The username ",
             mandatory = true)
    private String username;

    @Address(id = "apiUrl",
             name = "HomeConnect API URL",
             description = "Specifies the base URL for HomeConnect API requests",
             mandatory = false,
             valueDefault = API_BASE_URL)
    private String apiUrl = API_BASE_URL;

    private HomeConnectApiClient client;

    @Override
    protected void onConnect() throws ConnectionException {
        try {
            client = new HomeConnectApiClient(apiUrl, username);
            
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    protected Object onRead(List<HomeConnectChannel> channels, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
        
        long samplingTime = System.currentTimeMillis();
        
        for (HomeConnectChannel channel : channels) {
            logger.debug("Read channel \"{}\": {}@{}", channel.getId(), channel.getResource(), channel.getHomeApplianceId());
        	try {
                Value value;
                switch (channel.getResource()) {
                case FRIDGE_TEMPERATURE_SETPOINT:
                	value = new DoubleValue(client.getFridgeSetpointTemperature(channel.getHomeApplianceId()).getValueAsInt());
                	break;
                case FREEZER_TEMPERATURE_SETPOINT:
                	value = new DoubleValue(client.getFreezerSetpointTemperature(channel.getHomeApplianceId()).getValueAsInt());
                	break;
                case FRIDGE_SUPER_MODE:
                	value = new BooleanValue(client.getFridgeSuperMode(channel.getHomeApplianceId()).getValueAsBoolean());
                	break;
                case FREEZER_SUPER_MODE:
                	value = new BooleanValue(client.getFreezerSuperMode(channel.getHomeApplianceId()).getValueAsBoolean());
                	break;
                default:
                	logger.warn("Unable to read resource {}", channel.getResource());
                	
                	channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
                	continue;
                }
                channel.setRecord(new Record(value, samplingTime, Flag.VALID));
                
                logger.trace("Read value from channel \"{}\": {}", channel.getId(), value);
                
			} catch (HomeConnectException e) {
				throw new ConnectionException(MessageFormat.format("Error reading channel {0}: {1}", 
						channel.getId(), e.getMessage()));
			}
        }
        return null;
    }

    @Override
    protected Object onWrite(List<HomeConnectChannel> channels, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        
        for (HomeConnectChannel channel : channels) {
            logger.debug("Write channel \"{}\": {}@{}", channel.getId(), channel.getResource(), channel.getHomeApplianceId());
        	try {
                Value value = channel.getValue();
                switch (channel.getResource()) {
                case FRIDGE_TEMPERATURE_SETPOINT:
                    client.setFridgeSetpointTemperature(channel.getHomeApplianceId(), value.asString(), "°C");
                	break;
                case FREEZER_TEMPERATURE_SETPOINT:
                    client.setFreezerSetpointTemperature(channel.getHomeApplianceId(), value.asString(), "°C");
                	break;
                case FRIDGE_SUPER_MODE:
                    client.setFridgeSuperMode(channel.getHomeApplianceId(), value.asBoolean());
                	break;
                case FREEZER_SUPER_MODE:
                    client.setFreezerSuperMode(channel.getHomeApplianceId(), value.asBoolean());
                	break;
                default:
                	logger.warn("Unable to write to resource {}", channel.getResource());
                	
                	channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
                	continue;
                }
                channel.setFlag(Flag.VALID);
                
                logger.trace("Wrote value to channel \"{}\": {}", channel.getId(), value);
                
			} catch (HomeConnectException e) {
				throw new ConnectionException(MessageFormat.format("Error writing to channel {0}: {1}", 
						channel.getId(), e.getMessage()));
			}
        }
        return null;
    }

}
