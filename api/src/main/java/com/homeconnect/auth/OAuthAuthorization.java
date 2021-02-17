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

import static com.homeconnect.data.Constants.OAUTH_SCOPE;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.homeconnect.client.exception.AuthorizationException;
import com.homeconnect.client.exception.HomeConnectException;

public class OAuthAuthorization implements Serializable{
    private static final long serialVersionUID = 1L;

    /** Directory to store user credentials. */
    public static final File DATA_STORE_DIR =
            new File(System.getProperty("user.home"), ".store/credential_storage");

    /**
    * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
    * globally shared instance across your application.
    */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** OAuth 2 scope. */
    private static final String SCOPE = OAUTH_SCOPE;

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**For simulator. */
    private static final String TOKEN_SERVER_URL = "https://simulator.home-connect.com/security/oauth/token";
    private static final String AUTHORIZATION_SERVER_URL = "https://simulator.home-connect.com/security/oauth/authorize";

    /**For physical device. */
//    private static final String TOKEN_SERVER_URL = "https://api.home-connect.com/security/oauth/token";
//    private static final String AUTHORIZATION_SERVER_URL = "https://api.home-connect.com/security/oauth/authorize";

    /** Object attributes.*/
    private  String username;
    private  String host;
    private  int    port;

    private  String apiKey;
    private  String apiSecret;

    public OAuthAuthorization(String username, String host, int port, String apiKey, String apiSecret, FileDataStoreFactory DATA_STORE_FACTORY) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        
        OAuthAuthorization.DATA_STORE_FACTORY = DATA_STORE_FACTORY;
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    public Credential authorize() throws AuthorizationException {
        try {
            // Set up authorization code flow
            AuthorizationCodeFlow flow =
                    new AuthorizationCodeFlow.Builder(
                            BearerToken.authorizationHeaderAccessMethod(),
                            HTTP_TRANSPORT,
                            JSON_FACTORY,
                            new GenericUrl(TOKEN_SERVER_URL),
                            new ClientParametersAuthentication(
                                    apiKey, apiSecret),
                            apiKey,
                            AUTHORIZATION_SERVER_URL)
                    .setScopes(Arrays.asList(SCOPE))
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .build();
            
            // Authorize
            LocalServerReceiver receiver =
                    new LocalServerReceiver.Builder()
                    .setHost(host)
                    .setPort(port)
                    .build();
            
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(username);
            
        } catch(Exception e) {
            throw new AuthorizationException("Authorization not successfull!");
        }
    }

    /** Get the username during runtime for verification.*/
    public String getUsername() {
        return this.username;
    }

    /** Set DataStoreFactory after reserialization.*/
    public OAuthAuthorization setDataStoreFactory(FileDataStoreFactory DATA_STORE_FACTORY) {
        OAuthAuthorization.DATA_STORE_FACTORY = DATA_STORE_FACTORY;
        return this;
    }

    public static Credential getCredentials(String username) throws AuthorizationException {
        try {
        	FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
	        
	        /** Read the stored authorization client out of file.*/
        	OAuthSerializer serializer = new OAuthSerializer();
	        OAuthAuthorization authorization = serializer.readObject(username);
	        
	        /** Set the Data_Store_Factory, because it is not serializable.*/
	        authorization.setDataStoreFactory(dataStoreFactory);
	        
	        /** Get Credentials for authorization client. */
	        return authorization.authorize();
			
		} catch (IOException e) {
			throw new AuthorizationException(e);
		}
    }

    public static Credential createCredentials(String username, String apiKey, String apiSecret) throws AuthorizationException {
    	try {
        	FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
            
            /** User specific information given in batch file for initialization. 
                IP and port of jetty server are hardcoded. */
        	OAuthSerializer serializer = new OAuthSerializer();
        	OAuthAuthorization authorization = new OAuthAuthorization(username, "127.0.0.1", 8085, apiKey, apiSecret, dataStoreFactory);
        	
            /** Store authorization client in file.*/
			serializer.writeObject(username, authorization);
			
	        /** Order credentials and store in file.*/
        	return authorization.authorize();
			
		} catch (IOException e) {
			throw new AuthorizationException(e);
		}
    }

    /**
     * Initial main method, to register a user for authorization.
     * 
     * @param args Username, Client - ID and Client - Secret
     * 
     * @throws HomeConnectException
     */
    public static void main(String[] args) throws HomeConnectException {
    	String username  = args[0];
    	String apiKey    = args[1];
    	String apiSecret = args[2];
        try {
        	Credential credentials = createCredentials(username, apiKey, apiSecret);
        	
	        /**Check if authorization worked.*/
	        if(credentials != null) {
	            System.out.println("\n"+"User "+ username + " registered:\n");
	        }
            /** Print out information for user in cmd Window.*/
            System.out.println("API KEY: "      + apiKey + "\n");
            System.out.println("API SECRET: "   + apiSecret + "\n");
            
        } catch (Exception e) {
            System.err.println("Authorization not succesfull: " + e.getMessage());
        }
    }

}
