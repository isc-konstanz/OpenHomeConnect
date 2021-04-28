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

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.Device;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.options.Address;
import org.openmuc.framework.options.AddressSyntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.homeconnect.client.HomeConnectApiClient;
import com.homeconnect.client.exception.HomeConnectException;
import com.homeconnect.client.exception.InvalidScopeOrIdException;

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
        try {
			for (HomeConnectChannel channel : channels) {
				logger.debug("Read channel \"{}\": {}@{}", channel.getId(), channel.getResource(),
						channel.getHomeApplianceId());
				try {
					channel.setData(client.get(channel.getHomeApplianceId(), channel.getResource()), samplingTime);
					logger.trace("Read value from channel \"{}\": {}", channel.getId(), channel.getValue());       
					
				} catch (UnsupportedOperationException e) {
					channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
                    logger.warn("Unable to read resource {}", channel.getResource());
				} catch (InvalidScopeOrIdException e) {
				    channel.setFlag(Flag.DRIVER_ERROR_READ_FAILURE);
				    logger.warn("Wrong scope or haId configured for resource {}", channel.getResource());
			    }
			}
        } catch(HomeConnectException e) {
        	throw new ConnectionException(
        			MessageFormat.format("Error reading channel! {0}", e.getMessage()));
        }
        return null;
    }

    @Override
    protected Object onWrite(List<HomeConnectChannel> channels, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {
        try {
			for (HomeConnectChannel channel : channels) {
				logger.debug("Write channel \"{}\": {}@{}", channel.getId(), channel.getResource(),
						channel.getHomeApplianceId());
				try {
					Value value = channel.getValue();
					client.set(channel.getHomeApplianceId(), channel.getResource(), value.asString(),
							channel.getUnit());

					channel.setFlag(Flag.VALID);
					logger.trace("Wrote value to channel \"{}\": {}", channel.getId(), value);

				} catch (UnsupportedOperationException e) {
					channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
					logger.warn("Unable to write resource {}", channel.getResource());
				} catch (InvalidScopeOrIdException e) {
					channel.setFlag(Flag.DRIVER_ERROR_READ_FAILURE);
					logger.warn("Wrong scope or haId configured for resource {}", channel.getResource());
				}			
			}
        } catch (HomeConnectException e) {
        	throw new ConnectionException(
        			MessageFormat.format("Error reading channel! {0}", e.getMessage()));
        }
        return null;
    }

}
