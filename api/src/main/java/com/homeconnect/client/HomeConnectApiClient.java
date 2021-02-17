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
package com.homeconnect.client;

import static com.homeconnect.client.OkHttpHelper.formatJsonBody;
import static com.homeconnect.client.OkHttpHelper.requestBuilder;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.homeconnect.auth.OAuthAuthorization;
import com.homeconnect.client.exception.ApplianceOfflineException;
import com.homeconnect.client.exception.AuthorizationException;
import com.homeconnect.client.exception.CommunicationException;
import com.homeconnect.client.exception.HomeConnectException;
import com.homeconnect.client.model.ApiRequest;
import com.homeconnect.client.model.AvailableProgram;
import com.homeconnect.client.model.AvailableProgramOption;
import com.homeconnect.client.model.Data;
import com.homeconnect.client.model.HomeAppliance;
import com.homeconnect.client.model.HomeConnectRequest;
import com.homeconnect.client.model.HomeConnectResponse;
import com.homeconnect.client.model.Option;
import com.homeconnect.client.model.Program;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for Home Connect API.
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
@NonNullByDefault
public class HomeConnectApiClient {

    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String BSH_JSON_V1 = "application/vnd.bsh.sdk.v1+json";
    private static final MediaType BSH_JSON_V1_MEDIA_TYPE = requireNonNull(MediaType.parse(BSH_JSON_V1));
    private static final int VALUE_TYPE_STRING = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_BOOLEAN = 2;
    private static final int COMMUNICATION_QUEUE_SIZE = 50;

    private final Logger logger;
    private final String apiUrl;
    private final OkHttpClient client;
    private final Credential credential;
    private final JsonParser jsonParser;

    private final Queue<ApiRequest> communicationQueue;

    private final Map<String, List<AvailableProgramOption>> availableProgramOptionsCache;

    public HomeConnectApiClient(String apiUrl, String username) throws AuthorizationException {
    	this(apiUrl, OAuthAuthorization.getCredentials(username), null);
    }

    public HomeConnectApiClient(String apiUrl, Credential credential,
            @Nullable List<ApiRequest> apiRequestHistory) {
        
        this.apiUrl = apiUrl;
        this.credential = credential;
        
        client = new OkHttpClient();
        
        jsonParser = new JsonParser();
        communicationQueue = QueueUtils.synchronizedQueue(new CircularFifoQueue<>(COMMUNICATION_QUEUE_SIZE));
        availableProgramOptionsCache = new ConcurrentHashMap<>();
        
        if (apiRequestHistory != null) {
            communicationQueue.addAll(apiRequestHistory);
        }
        logger = LoggerFactory.getLogger(HomeConnectApiClient.class);
    }

    /**
     * Get all home appliances
     *
     * @return list of {@link HomeAppliance}
     * @throws HomeConnectException 
     */
    public List<HomeAppliance> getHomeAppliances() throws HomeConnectException {
        Request request = createGetRequest("/api/homeappliances");
        try (Response response = client.newCall(request).execute();) {
            checkResponseCode(HTTP_OK, request, response, null, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(null, request, null, response, responseBody);

            return mapToHomeAppliances(responseBody);
        } catch (IOException | ApplianceOfflineException e) {
            logger.warn("Failed to fetch home appliances! error={}", e.getMessage());
            trackAndLogApiRequest(null, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    /**
     * Get home appliance by id
     *
     * @param haId home appliance id
     * @return {@link HomeAppliance}
     * @throws HomeConnectException
     */
    public HomeAppliance getHomeAppliance(String haId) throws HomeConnectException {
        Request request = createGetRequest("/api/homeappliances/" + haId);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_OK, request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            return mapToHomeAppliance(responseBody);
        } catch (IOException | ApplianceOfflineException e) {
            logger.warn("Failed to get home appliance! haId={}, error={}", haId, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    /**
     * Get ambient light state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getAmbientLightState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "BSH.Common.Setting.AmbientLightEnabled");
    }

    /**
     * Set ambient light state of device.
     *
     * @param haId home appliance id
     * @param enable enable or disable ambient light
     * @throws HomeConnectException
     */
    public void setAmbientLightState(String haId, boolean enable)
            throws HomeConnectException {
        putSettings(haId, new Data("BSH.Common.Setting.AmbientLightEnabled", String.valueOf(enable), null),
                VALUE_TYPE_BOOLEAN);
    }

    /**
     * Get functional light state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getFunctionalLightState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Cooking.Common.Setting.Lighting");
    }

    /**
     * Set functional light state of device.
     *
     * @param haId home appliance id
     * @param enable enable or disable functional light
     * @throws HomeConnectException
     */
    public void setFunctionalLightState(String haId, boolean enable)
            throws HomeConnectException {
        putSettings(haId, new Data("Cooking.Common.Setting.Lighting", String.valueOf(enable), null),
                VALUE_TYPE_BOOLEAN);
    }

    /**
     * Get functional light brightness state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getFunctionalLightBrightnessState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Cooking.Common.Setting.LightingBrightness");
    }

    /**
     * Set functional light brightness of device.
     *
     * @param haId home appliance id
     * @param value brightness value 10-100
     * @throws HomeConnectException
     */
    public void setFunctionalLightBrightnessState(String haId, int value)
            throws HomeConnectException {
        putSettings(haId, new Data("Cooking.Common.Setting.LightingBrightness", String.valueOf(value), "%"),
                VALUE_TYPE_INT);
    }

    /**
     * Get ambient light brightness state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getAmbientLightBrightnessState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "BSH.Common.Setting.AmbientLightBrightness");
    }

    /**
     * Set ambient light brightness of device.
     *
     * @param haId home appliance id
     * @param value brightness value 10-100
     * @throws HomeConnectException
     */
    public void setAmbientLightBrightnessState(String haId, int value)
            throws HomeConnectException {
        putSettings(haId, new Data("BSH.Common.Setting.AmbientLightBrightness", String.valueOf(value), "%"),
                VALUE_TYPE_INT);
    }

    /**
     * Get ambient light color state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getAmbientLightColorState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "BSH.Common.Setting.AmbientLightColor");
    }

    /**
     * Set ambient light color of device.
     *
     * @param haId home appliance id
     * @param value color code
     * @throws HomeConnectException
     */
    public void setAmbientLightColorState(String haId, String value)
            throws HomeConnectException {
        putSettings(haId, new Data("BSH.Common.Setting.AmbientLightColor", value, null));
    }

    /**
     * Get ambient light custom color state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getAmbientLightCustomColorState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "BSH.Common.Setting.AmbientLightCustomColor");
    }

    /**
     * Set ambient light color of device.
     *
     * @param haId home appliance id
     * @param value color code
     * @throws HomeConnectException
     */
    public void setAmbientLightCustomColorState(String haId, String value)
            throws HomeConnectException {
        putSettings(haId, new Data("BSH.Common.Setting.AmbientLightCustomColor", value, null));
    }

    /**
     * Get power state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getPowerState(String haId)
            throws HomeConnectException {
        return getSetting(haId, "BSH.Common.Setting.PowerState");
    }

    /**
     * Set power state of device.
     *
     * @param haId home appliance id
     * @param state target state
     * @throws HomeConnectException
     */
    public void setPowerState(String haId, String state)
            throws HomeConnectException {
        putSettings(haId, new Data("BSH.Common.Setting.PowerState", state, null));
    }

    /**
     * Get setpoint temperature of freezer
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getFreezerSetpointTemperature(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Refrigeration.FridgeFreezer.Setting.SetpointTemperatureFreezer");
    }

    /**
     * Set setpoint temperature of freezer
     *
     * @param haId home appliance id
     * @param state new temperature
     * @throws HomeConnectException
     */
    public void setFreezerSetpointTemperature(String haId, String state, String unit)
            throws HomeConnectException {
        putSettings(haId, new Data("Refrigeration.FridgeFreezer.Setting.SetpointTemperatureFreezer", state, unit),
                VALUE_TYPE_INT);
    }

    /**
     * Get setpoint temperature of fridge
     *
     * @param haId home appliance id
     * @return {@link Data} or null in case of communication error
     * @throws HomeConnectException
     */
    public Data getFridgeSetpointTemperature(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Refrigeration.FridgeFreezer.Setting.SetpointTemperatureRefrigerator");
    }

    /**
     * Set setpoint temperature of fridge
     *
     * @param haId home appliance id
     * @param state new temperature
     * @throws HomeConnectException
     */
    public void setFridgeSetpointTemperature(String haId, String state, String unit)
            throws HomeConnectException {
        putSettings(haId, new Data("Refrigeration.FridgeFreezer.Setting.SetpointTemperatureRefrigerator", state, unit),
                VALUE_TYPE_INT);
    }

    /**
     * Get fridge super mode
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getFridgeSuperMode(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Refrigeration.FridgeFreezer.Setting.SuperModeRefrigerator");
    }

    /**
     * Set fridge super mode
     *
     * @param haId home appliance id
     * @param enable enable or disable fridge super mode
     * @throws HomeConnectException
     */
    public void setFridgeSuperMode(String haId, boolean enable)
            throws HomeConnectException {
        putSettings(haId,
                new Data("Refrigeration.FridgeFreezer.Setting.SuperModeRefrigerator", String.valueOf(enable), null),
                VALUE_TYPE_BOOLEAN);
    }

    /**
     * Get freezer super mode
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getFreezerSuperMode(String haId)
            throws HomeConnectException {
        return getSetting(haId, "Refrigeration.FridgeFreezer.Setting.SuperModeFreezer");
    }

    /**
     * Set freezer super mode
     *
     * @param haId home appliance id
     * @param enable enable or disable freezer super mode
     * @throws HomeConnectException
     */
    public void setFreezerSuperMode(String haId, boolean enable)
            throws HomeConnectException {
        putSettings(haId,
                new Data("Refrigeration.FridgeFreezer.Setting.SuperModeFreezer", String.valueOf(enable), null),
                VALUE_TYPE_BOOLEAN);
    }

    /**
     * Get door state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getDoorState(String haId)
            throws HomeConnectException {
        return getStatus(haId, "BSH.Common.Status.DoorState");
    }

    /**
     * Get operation state of device.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getOperationState(String haId)
            throws HomeConnectException {
        return getStatus(haId, "BSH.Common.Status.OperationState");
    }

    /**
     * Get current cavity temperature of oven.
     *
     * @param haId home appliance id
     * @return {@link Data}
     * @throws HomeConnectException
     */
    public Data getCurrentCavityTemperature(String haId)
            throws HomeConnectException {
        return getStatus(haId, "Cooking.Oven.Status.CurrentCavityTemperature");
    }

    /**
     * Is remote start allowed?
     *
     * @param haId haId home appliance id
     * @return true or false
     * @throws HomeConnectException
     */
    public boolean isRemoteControlStartAllowed(String haId)
            throws HomeConnectException {
        Data data = getStatus(haId, "BSH.Common.Status.RemoteControlStartAllowed");
        return "true".equalsIgnoreCase(data.getValue());
    }

    /**
     * Is remote control allowed?
     *
     * @param haId haId home appliance id
     * @return true or false
     * @throws HomeConnectException
     */
    public boolean isRemoteControlActive(String haId)
            throws HomeConnectException {
        Data data = getStatus(haId, "BSH.Common.Status.RemoteControlActive");
        return "true".equalsIgnoreCase(data.getValue());
    }

    /**
     * Is local control allowed?
     *
     * @param haId haId home appliance id
     * @return true or false
     * @throws HomeConnectException
     */
    public boolean isLocalControlActive(String haId)
            throws HomeConnectException {
        Data data = getStatus(haId, "BSH.Common.Status.LocalControlActive");
        return "true".equalsIgnoreCase(data.getValue());
    }

    /**
     * Get active program of device.
     *
     * @param haId home appliance id
     * @return {@link Data} or null if there is no active program
     * @throws HomeConnectException
     */
    public @Nullable Program getActiveProgram(String haId)
            throws HomeConnectException {
        return getProgram(haId, "/api/homeappliances/" + haId + "/programs/active");
    }

    /**
     * Get selected program of device.
     *
     * @param haId home appliance id
     * @return {@link Data} or null if there is no selected program
     * @throws HomeConnectException
     */
    public @Nullable Program getSelectedProgram(String haId)
            throws HomeConnectException {
        return getProgram(haId, "/api/homeappliances/" + haId + "/programs/selected");
    }

    public void setSelectedProgram(String haId, String program)
            throws HomeConnectException {
        putData(haId, "/api/homeappliances/" + haId + "/programs/selected", new Data(program, null, null),
                VALUE_TYPE_STRING);
    }

    public void startProgram(String haId, String program)
            throws HomeConnectException {
        putData(haId, "/api/homeappliances/" + haId + "/programs/active", new Data(program, null, null),
                VALUE_TYPE_STRING);
    }

    public void startSelectedProgram(String haId)
            throws HomeConnectException {
        @Nullable
        String selectedProgram = getRaw(haId, "/api/homeappliances/" + haId + "/programs/selected");
        if (selectedProgram != null) {
            putRaw(haId, "/api/homeappliances/" + haId + "/programs/active", selectedProgram);
        }
    }

    public void startCustomProgram(String haId, String json)
            throws HomeConnectException {
        putRaw(haId, "/api/homeappliances/" + haId + "/programs/active", json);
    }

    public void setProgramOptions(String haId, String key, String value, @Nullable String unit, boolean valueAsInt,
            boolean isProgramActive) throws HomeConnectException {
        String programState = isProgramActive ? "active" : "selected";

        putOption(haId, "/api/homeappliances/" + haId + "/programs/" + programState + "/options",
                new Option(key, value, unit), valueAsInt);
    }

    public void stopProgram(String haId)
            throws HomeConnectException {
        sendDelete(haId, "/api/homeappliances/" + haId + "/programs/active");
    }

    public List<AvailableProgram> getPrograms(String haId)
            throws HomeConnectException {
        return getAvailablePrograms(haId, "/api/homeappliances/" + haId + "/programs");
    }

    public List<AvailableProgram> getAvailablePrograms(String haId)
            throws HomeConnectException {
        return getAvailablePrograms(haId, "/api/homeappliances/" + haId + "/programs/available");
    }

    public List<AvailableProgramOption> getProgramOptions(String haId, String programKey)
            throws HomeConnectException {
        if (availableProgramOptionsCache.containsKey(programKey)) {
            logger.debug("Returning cached options for '{}'.", programKey);
            List<AvailableProgramOption> availableProgramOptions = availableProgramOptionsCache.get(programKey);
            return availableProgramOptions != null ? availableProgramOptions : Collections.emptyList();
        }

        String path = "/api/homeappliances/" + haId + "/programs/available/" + programKey;
        Request request = createGetRequest(path);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_OK, request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            List<AvailableProgramOption> availableProgramOptions = mapToAvailableProgramOption(responseBody, haId);
            availableProgramOptionsCache.put(programKey, availableProgramOptions);
            return availableProgramOptions;
        } catch (IOException e) {
            logger.warn("Failed to get program options! haId={}, programKey={}, error={}", haId, programKey,
                    e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    /**
     * Get latest API requests.
     *
     * @return thread safe queue
     */
    public Queue<ApiRequest> getLatestApiRequests() {
        return communicationQueue;
    }

    public Data getSetting(String haId, String setting)
            throws HomeConnectException {
        return getData(haId, "/api/homeappliances/" + haId + "/settings/" + setting);
    }

    public void putSettings(String haId, Data data)
            throws HomeConnectException {
        putSettings(haId, data, VALUE_TYPE_STRING);
    }

    public void putSettings(String haId, Data data, int valueType)
            throws HomeConnectException {
        putData(haId, "/api/homeappliances/" + haId + "/settings/" + data.getName(), data, valueType);
    }

    public Data getStatus(String haId, String status)
            throws HomeConnectException {
        return getData(haId, "/api/homeappliances/" + haId + "/status/" + status);
    }

    private @Nullable String getRaw(String haId, String path)
            throws HomeConnectException {
        return getRaw(haId, path, false);
    }

    private @Nullable String getRaw(String haId, String path, boolean ignoreResponseCode)
            throws HomeConnectException {
        Request request = createGetRequest(path);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_OK, request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            if (ignoreResponseCode || response.code() == HTTP_OK) {
                return responseBody;
            }
        } catch (IOException e) {
            logger.warn("Failed to get raw! haId={}, path={}, error={}", haId, path, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
        return null;
    }

    private String putRaw(String haId, String path, String requestBodyPayload)
            throws HomeConnectException {
        @Deprecated
        RequestBody requestBody = RequestBody.create(BSH_JSON_V1_MEDIA_TYPE,
                requestBodyPayload.getBytes(StandardCharsets.UTF_8));

        Request request = requestBuilder(credential).url(apiUrl + path).header(CONTENT_TYPE, BSH_JSON_V1)
                .header(ACCEPT, BSH_JSON_V1).put(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_NO_CONTENT, request, response, haId, requestBodyPayload);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, requestBodyPayload, response, responseBody);
            return responseBody;
        } catch (IOException e) {
            logger.warn("Failed to put raw! haId={}, path={}, payload={}, error={}", haId, path, requestBodyPayload,
                    e.getMessage());
            trackAndLogApiRequest(haId, request, requestBodyPayload, null, null);
            throw new CommunicationException(e);
        }
    }

    private @Nullable Program getProgram(String haId, String path)
            throws HomeConnectException {
        Request request = createGetRequest(path);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(asList(HTTP_OK, HTTP_NOT_FOUND), request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            if (response.code() == HTTP_OK) {
                return mapToProgram(responseBody);
            }
        } catch (IOException e) {
            logger.warn("Failed to get program! haId={}, path={}, error={}", haId, path, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
        return null;
    }

    private List<AvailableProgram> getAvailablePrograms(String haId, String path)
            throws HomeConnectException {
        Request request = createGetRequest(path);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_OK, request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            return mapToAvailablePrograms(responseBody, haId);
        } catch (IOException e) {
            logger.warn("Failed to get available programs! haId={}, path={}, error={}", haId, path, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    private void sendDelete(String haId, String path)
            throws HomeConnectException {
        Request request = requestBuilder(credential).url(apiUrl + path).header(ACCEPT, BSH_JSON_V1).delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_NO_CONTENT, request, response, haId, null);

            trackAndLogApiRequest(haId, request, null, response, mapToString(response.body()));
        } catch (IOException e) {
            logger.warn("Failed to send delete! haId={}, path={}, error={}", haId, path, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    private Data getData(String haId, String path)
            throws HomeConnectException {
        Request request = createGetRequest(path);
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_OK, request, response, haId, null);

            String responseBody = mapToString(response.body());
            trackAndLogApiRequest(haId, request, null, response, responseBody);

            return mapToState(responseBody);
        } catch (IOException e) {
            logger.warn("Failed to get data! haId={}, path={}, error={}", haId, path, e.getMessage());
            trackAndLogApiRequest(haId, request, null, null, null);
            throw new CommunicationException(e);
        }
    }

    private void putData(String haId, String path, Data data, int valueType)
            throws HomeConnectException {
        JsonObject innerObject = new JsonObject();
        innerObject.addProperty("key", data.getName());

        if (data.getValue() != null) {
            if (valueType == VALUE_TYPE_INT) {
                innerObject.addProperty("value", data.getValueAsInt());
            } else if (valueType == VALUE_TYPE_BOOLEAN) {
                innerObject.addProperty("value", data.getValueAsBoolean());
            } else {
                innerObject.addProperty("value", data.getValue());
            }
        }

        if (data.getUnit() != null) {
            innerObject.addProperty("unit", data.getUnit());
        }

        JsonObject dataObject = new JsonObject();
        dataObject.add("data", innerObject);
        String requestBodyPayload = dataObject.toString();

        RequestBody requestBody = RequestBody.create(BSH_JSON_V1_MEDIA_TYPE,
                requestBodyPayload.getBytes(StandardCharsets.UTF_8));

        Request request = requestBuilder(credential).url(apiUrl + path).header(CONTENT_TYPE, BSH_JSON_V1)
                .header(ACCEPT, BSH_JSON_V1).put(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_NO_CONTENT, request, response, haId, requestBodyPayload);

            trackAndLogApiRequest(haId, request, requestBodyPayload, response, mapToString(response.body()));
        } catch (IOException e) {
            logger.warn("Failed to put data! haId={}, path={}, data={}, valueType={}, error={}", haId, path, data,
                    valueType, e.getMessage());
            trackAndLogApiRequest(haId, request, requestBodyPayload, null, null);
            throw new CommunicationException(e);
        }
    }

    private void putOption(String haId, String path, Option option, boolean asInt)
            throws HomeConnectException {
        JsonObject innerObject = new JsonObject();
        innerObject.addProperty("key", option.getKey());

        if (option.getValue() != null) {
            if (asInt) {
                innerObject.addProperty("value", option.getValueAsInt());
            } else {
                innerObject.addProperty("value", option.getValue());
            }
        }

        if (option.getUnit() != null) {
            innerObject.addProperty("unit", option.getUnit());
        }

        JsonArray optionsArray = new JsonArray();
        optionsArray.add(innerObject);

        JsonObject optionsObject = new JsonObject();
        optionsObject.add("options", optionsArray);

        JsonObject dataObject = new JsonObject();
        dataObject.add("data", optionsObject);

        String requestBodyPayload = dataObject.toString();

        RequestBody requestBody = RequestBody.create(BSH_JSON_V1_MEDIA_TYPE,
                requestBodyPayload.getBytes(StandardCharsets.UTF_8));

        Request request = requestBuilder(credential).url(apiUrl + path).header(CONTENT_TYPE, BSH_JSON_V1)
                .header(ACCEPT, BSH_JSON_V1).put(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            checkResponseCode(HTTP_NO_CONTENT, request, response, haId, requestBodyPayload);

            trackAndLogApiRequest(haId, request, requestBodyPayload, response, mapToString(response.body()));
        } catch (IOException e) {
            logger.warn("Failed to put option! haId={}, path={}, option={}, asInt={}, error={}", haId, path, option,
                    asInt, e.getMessage());
            trackAndLogApiRequest(haId, request, requestBodyPayload, null, null);
            throw new CommunicationException(e);
        }
    }

    private void checkResponseCode(int desiredCode, Request request, Response response, @Nullable String haId,
            @Nullable String requestPayload)
            throws CommunicationException, AuthorizationException, ApplianceOfflineException {
        checkResponseCode(singletonList(desiredCode), request, response, haId, requestPayload);
    }

    private void checkResponseCode(List<Integer> desiredCodes, Request request, Response response,
            @Nullable String haId, @Nullable String requestPayload)
            throws CommunicationException, AuthorizationException, ApplianceOfflineException {

        if (!desiredCodes.contains(HTTP_UNAUTHORIZED) && response.code() == HTTP_UNAUTHORIZED) {
            logger.debug("Current access token is invalid.");
            String responseBody = "";
            try {
                responseBody = mapToString(response.body());
            } catch (IOException e) {
                logger.error("Could not get HTTP response body as string.", e);
            }
            trackAndLogApiRequest(haId, request, requestPayload, response, responseBody);
            throw new AuthorizationException("Token invalid!");
        }

        if (!desiredCodes.contains(response.code())) {
            int code = response.code();
            String message = response.message();

            logger.debug("Invalid HTTP response code {} (allowed: {})", code, desiredCodes);
            String responseBody = "";
            try {
                responseBody = mapToString(response.body());
            } catch (IOException e) {
                logger.error("Could not get HTTP response body as string.", e);
            }
            trackAndLogApiRequest(haId, request, requestPayload, response, responseBody);

            if (code == HTTP_CONFLICT && containsIgnoreCase(responseBody, "error")
                    && containsIgnoreCase(responseBody, "offline")) {
                throw new ApplianceOfflineException(code, message, responseBody);
            } else {
                throw new CommunicationException(code, message, responseBody);
            }
        }
    }

    private String mapToString(@Nullable ResponseBody responseBody) throws IOException {
        if (responseBody != null) {
            return responseBody.string();
        }
        return "";
    }

    private Program mapToProgram(String json) {
        ArrayList<Option> optionList = new ArrayList<>();
        JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();
        JsonObject data = responseObject.getAsJsonObject("data");
        Program result = new Program(data.get("key").getAsString(), optionList);
        JsonArray options = data.getAsJsonArray("options");

        options.forEach(option -> {
            JsonObject obj = (JsonObject) option;

            @Nullable
            String key = obj.get("key") != null ? obj.get("key").getAsString() : null;
            @Nullable
            String value = obj.get("value") != null && !obj.get("value").isJsonNull() ? obj.get("value").getAsString()
                    : null;
            @Nullable
            String unit = obj.get("unit") != null ? obj.get("unit").getAsString() : null;

            optionList.add(new Option(key, value, unit));
        });

        return result;
    }

    private List<AvailableProgram> mapToAvailablePrograms(String json, String haId) {
        ArrayList<AvailableProgram> result = new ArrayList<>();

        try {
            JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();

            JsonArray programs = responseObject.getAsJsonObject("data").getAsJsonArray("programs");
            programs.forEach(program -> {
                JsonObject obj = (JsonObject) program;
                @Nullable
                String key = obj.get("key") != null ? obj.get("key").getAsString() : null;
                JsonObject constraints = obj.getAsJsonObject("constraints");
                boolean available = constraints.get("available") != null && constraints.get("available").getAsBoolean();
                @Nullable
                String execution = constraints.get("execution") != null ? constraints.get("execution").getAsString()
                        : null;

                if (key != null && execution != null) {
                    result.add(new AvailableProgram(key, available, execution));
                }
            });
        } catch (Exception e) {
            logger.error("Could not parse available programs response! haId={}, error={}", haId, e.getMessage());
        }

        return result;
    }

    private List<AvailableProgramOption> mapToAvailableProgramOption(String json, String haId) {
        ArrayList<AvailableProgramOption> result = new ArrayList<>();

        try {
            JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();

            JsonArray options = responseObject.getAsJsonObject("data").getAsJsonArray("options");
            options.forEach(option -> {
                JsonObject obj = (JsonObject) option;
                @Nullable
                String key = obj.get("key") != null ? obj.get("key").getAsString() : null;
                ArrayList<String> allowedValues = new ArrayList<>();
                obj.getAsJsonObject("constraints").getAsJsonArray("allowedvalues")
                        .forEach(value -> allowedValues.add(value.getAsString()));

                if (key != null) {
                    result.add(new AvailableProgramOption(key, allowedValues));
                }
            });
        } catch (Exception e) {
            logger.warn("Could not parse available program options response! haId={}, error={}", haId, e.getMessage());
        }

        return result;
    }

    private HomeAppliance mapToHomeAppliance(String json) {
        JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();

        JsonObject data = responseObject.getAsJsonObject("data");

        return new HomeAppliance(data.get("haId").getAsString(), data.get("name").getAsString(),
                data.get("brand").getAsString(), data.get("vib").getAsString(), data.get("connected").getAsBoolean(),
                data.get("type").getAsString(), data.get("enumber").getAsString());
    }

    private ArrayList<HomeAppliance> mapToHomeAppliances(String json) {
        final ArrayList<HomeAppliance> result = new ArrayList<>();
        JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();

        JsonObject data = responseObject.getAsJsonObject("data");
        JsonArray homeappliances = data.getAsJsonArray("homeappliances");

        homeappliances.forEach(appliance -> {
            JsonObject obj = (JsonObject) appliance;

            result.add(new HomeAppliance(obj.get("haId").getAsString(), obj.get("name").getAsString(),
                    obj.get("brand").getAsString(), obj.get("vib").getAsString(), obj.get("connected").getAsBoolean(),
                    obj.get("type").getAsString(), obj.get("enumber").getAsString()));
        });

        return result;
    }

    private Data mapToState(String json) {
        JsonObject responseObject = jsonParser.parse(json).getAsJsonObject();

        JsonObject data = responseObject.getAsJsonObject("data");

        @Nullable
        String unit = data.get("unit") != null ? data.get("unit").getAsString() : null;

        return new Data(data.get("key").getAsString(), data.get("value").getAsString(), unit);
    }

    private Request createGetRequest(String path) throws HomeConnectException {
        return requestBuilder(credential).url(apiUrl + path).header(ACCEPT, BSH_JSON_V1).get().build();
    }

    private void trackAndLogApiRequest(@Nullable String haId, Request request, @Nullable String requestBody,
            @Nullable Response response, @Nullable String responseBody) {
        HomeConnectRequest homeConnectRequest = map(request, requestBody);
        @Nullable
        HomeConnectResponse homeConnectResponse = response != null ? map(response, responseBody) : null;

        logApiRequest(haId, homeConnectRequest, homeConnectResponse);
        trackApiRequest(homeConnectRequest, homeConnectResponse);
    }

    private void logApiRequest(@Nullable String haId, HomeConnectRequest homeConnectRequest,
            @Nullable HomeConnectResponse homeConnectResponse) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();

            if (haId != null) {
                sb.append("[").append(haId).append("] ");
            }

            sb.append(homeConnectRequest.getMethod()).append(" ");
            if (homeConnectResponse != null) {
                sb.append(homeConnectResponse.getCode()).append(" ");
            }
            sb.append(homeConnectRequest.getUrl()).append("\n");
            homeConnectRequest.getHeader()
                    .forEach((key, value) -> sb.append("> ").append(key).append(": ").append(value).append("\n"));

            if (homeConnectRequest.getBody() != null) {
                sb.append(homeConnectRequest.getBody()).append("\n");
            }

            if (homeConnectResponse != null) {
                sb.append("\n");
                homeConnectResponse.getHeader()
                        .forEach((key, value) -> sb.append("< ").append(key).append(": ").append(value).append("\n"));
            }
            if (homeConnectResponse != null && homeConnectResponse.getBody() != null) {
                sb.append(homeConnectResponse.getBody()).append("\n");
            }

            logger.debug("{}", sb.toString());
        }
    }

    private void trackApiRequest(HomeConnectRequest homeConnectRequest,
            @Nullable HomeConnectResponse homeConnectResponse) {
        communicationQueue.add(new ApiRequest(ZonedDateTime.now(), homeConnectRequest, homeConnectResponse));
    }

    private HomeConnectRequest map(Request request, @Nullable String requestBody) {
        HashMap<String, String> headers = new HashMap<>();
        request.headers().toMultimap().forEach((key, values) -> headers.put(key, values.toString()));

        return new HomeConnectRequest(request.url().toString(), request.method(), headers,
                requestBody != null ? formatJsonBody(requestBody) : null);
    }

    private HomeConnectResponse map(Response response, @Nullable String responseBody) {
        HashMap<String, String> headers = new HashMap<>();
        response.headers().toMultimap().forEach((key, values) -> headers.put(key, values.toString()));

        return new HomeConnectResponse(response.code(), headers,
                responseBody != null ? formatJsonBody(responseBody) : null);
    }

}
