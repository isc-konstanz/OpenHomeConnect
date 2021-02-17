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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.Channel;
import org.openmuc.framework.options.Address;
import org.openmuc.framework.options.AddressSyntax;

import com.homeconnect.data.Resource;

@AddressSyntax(separator = "@")
public class HomeConnectChannel extends Channel {

    private Resource resource;

    @Address(id = "resource",
             name = "Resource",
             description = "The resource of the home appliance, referenced by this channel",
             mandatory = true)
    private String resourceString;

    @Address(id = "haId",
             name = "Home appliance ID",
             description = "The unique home appliance ID",
             mandatory = true)
    private String haId;

    @Override
    protected void onConfigure() throws ArgumentSyntaxException {
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

}
