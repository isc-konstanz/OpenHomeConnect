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
package com.homeconnect.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthSerializer {

    private final static Logger logger = LoggerFactory.getLogger(OAuthSerializer.class);

    /**
     * Method to write authorizationClient in file for later use.
     * 
     * @param oauthAuthorization Authorization object consists of Username, Client - ID, Client - Secret
     * 
     * @throws FileNotFoundException Exception caused by missing file.
     * @throws IOException Exception caused by error during writing file.
     **/
    public void writeObject(String username, OAuthAuthorization oauthAuthorization, String storePath) throws FileNotFoundException, IOException {
        File file = new File(storePath+"/"+username+".ser");
        FileOutputStream fileStream = new FileOutputStream(file);
        ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
        try {
            objectStream.writeObject(oauthAuthorization);
            objectStream.flush();
            
        } finally {
            objectStream.close();
            fileStream .close();
        }
    }

    /**
     * Method to read authorizationClient out of file. 
     * 
     * @param username Username to find object
     * 
     * @return return authorization object
     * 
     * @throws FileNotFoundException Exception caused by missing file.
     * @throws IOException	Exception caused by error during reading file.
     */
    public OAuthAuthorization readObject(String username) throws FileNotFoundException, IOException {
    	File file = new File(System.getProperty(OAuthAuthorization.class.getPackage().getName().toLowerCase() + ".store",
				System.getProperty("user.home") + "/.store")+"/credential_storage/"+username+".ser");
        FileInputStream fileStream = new FileInputStream(file);
        ObjectInputStream objectStream = new ObjectInputStream(fileStream);    
        OAuthAuthorization auth;
        try {
            auth = (OAuthAuthorization) objectStream.readObject();
            
            if (!auth.getUsername().equals(username)) {
                logger.warn("User not registered: {}", username);
                
                throw new FileNotFoundException("File not found: " + file);
            }
            return auth; 
            
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading authorization: " + e.getMessage());
            
        } finally {
            objectStream.close();
            fileStream.close();
        }
    }

}

    

