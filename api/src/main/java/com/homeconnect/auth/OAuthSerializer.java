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

    /*
     * Method to write authorizationClient in file for later use.
     * 
     * @param oauthAuthorization Authorization object consists of Username, Client - ID, Client - Secret
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeObject(String username, OAuthAuthorization oauthAuthorization) throws FileNotFoundException, IOException {
        File file = new File(System.getProperty("user.home"), ".store/"+username+".ser");
        
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
     * @throws FileNotFoundException
     * @throws IOException
     */
    public OAuthAuthorization readObject(String username) throws FileNotFoundException, IOException {
        File file = new File(System.getProperty("user.home"), ".store/"+username+".ser");
        
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

    

