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

import static com.homeconnect.data.Constants.OAUTH_SCOPE;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
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
    public static final Path DATA_STORE_DIR = 
    		Path.of(System.getProperty(OAuthAuthorization.class.getPackage().getName().toLowerCase(),
    				System.getProperty("user.home")), ".auth");

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
//    private static final String TOKEN_SERVER_URL = "https://simulator.home-connect.com/security/oauth/token";
//    private static final String AUTHORIZATION_SERVER_URL = "https://simulator.home-connect.com/security/oauth/authorize";

    /**For physical device. */
    private static final String TOKEN_SERVER_URL = "https://api.home-connect.com/security/oauth/token";
    private static final String AUTHORIZATION_SERVER_URL = "https://api.home-connect.com/security/oauth/authorize";

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
     * @return returns Credential object
     * @throws AuthorizationException Exception caused by authorization error
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

    /** Get the username during runtime for verification.
     * @return returns the username of the client
     * */
    public String getUsername() {
        return this.username;
    }

    /** Set DataStoreFactory after reserialization.
     * @param DATA_STORE_FACTORY object to store credentials
     * @return returns DATA_STORE_FACTORY object*/
    public OAuthAuthorization setDataStoreFactory(FileDataStoreFactory DATA_STORE_FACTORY) {
        OAuthAuthorization.DATA_STORE_FACTORY = DATA_STORE_FACTORY;
        return this;
    }

    public static Credential getCredentials(String username) throws AuthorizationException {
        Credential credentials;
        try {
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR.toFile());
            
            /** Read the stored authorization client out of file.*/
            OAuthSerializer serializer = new OAuthSerializer();
            OAuthAuthorization authorization = serializer.readObject(username, DATA_STORE_DIR.toString());
            
            /** Set the Data_Store_Factory, because it is not serializable.*/
            authorization.setDataStoreFactory(dataStoreFactory);
            
            /** Get Credentials for authorization client. */
            credentials = authorization.authorize();
            if (credentials == null) {
            	throw new AuthorizationException("Unknown user: " + username);
            }
        } catch (IOException e) {
            throw new AuthorizationException(e);
        }
        return credentials;
    }

    public static Credential createCredentials(String username, String redirectURI, int port, String apiKey, String apiSecret, Path storePath) throws AuthorizationException {
        Credential credentials;
        try {
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(storePath.toFile());
            
            /** User specific information given in batch file for initialization. 
                IP and port of jetty server are hardcoded. */
            OAuthSerializer serializer = new OAuthSerializer();
            OAuthAuthorization authorization = new OAuthAuthorization(username, redirectURI, port, apiKey, apiSecret, dataStoreFactory);
            
            /** Store authorization client in file.*/
            serializer.writeObject(username, authorization, storePath.toString());
            
            /** Order credentials and store in file.*/
            credentials = authorization.authorize();
            
        } catch (IOException e) {
            throw new AuthorizationException(e);
        }
        return credentials;
    }

    /**
     * Initial main method, to register a user for authorization.
     * 
     * @param args Username, Client - ID and Client - Secret, redirectURI, port, storePath
     * 
     * @throws HomeConnectException Exception in HomeConnect interface
     */
    public static void main(String[] args) throws HomeConnectException {
        String username  = args[0];
        String apiKey    = args[1];
        String apiSecret = args[2];
        String redirectURI = args[3];
        
    	int port;
    	if (args.length < 5) {
    		port = 8085;
    	}
    	else {
    		port = Integer.parseInt(args[4]);
    	}
    	
    	Path storePath;
    	if (args.length < 6) {
    		storePath = DATA_STORE_DIR;
    	}
    	else {
    		storePath = Path.of(args[5]);
    	}
        try {
            Credential credentials = createCredentials(username, redirectURI, port, apiKey, apiSecret, storePath);
            
            /**Check if authorization worked.*/
            if (credentials != null) {
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
