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

import org.openmuc.framework.driver.DriverActivator;
import org.openmuc.framework.driver.annotation.Driver;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;


@Component
@Driver(id = HomeConnectDriver.ID, 
        name = HomeConnectDriver.NAME, description = HomeConnectDriver.DESCRIPTION,
        device = HomeConnection.class)
public class HomeConnectDriver extends DriverActivator implements DriverService {

    public static final String ID = "homeconnect";
    public static final String NAME = "HomeConnect";
    public static final String DESCRIPTION = "Implements the OAuth2 Home Connect API for the OpenMUC framework.";

}
