package org.openmuc.framework.driver.homeconnect;

import java.util.ArrayList;
import java.util.List;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.homeconnect.client.HomeConnectApiClient;
import com.homeconnect.client.exception.HomeConnectException;
import com.homeconnect.client.exception.InvalidScopeOrIdException;
import com.homeconnect.client.listener.HomeConnectEventListener;
import com.homeconnect.client.model.Data;
import com.homeconnect.client.model.Event;
import com.homeconnect.client.model.EventType;
import com.homeconnect.data.Constants;

public class HomeConnectListener extends Thread implements HomeConnectEventListener{

	private static final Logger logger = LoggerFactory.getLogger(HomeConnection.class);
	
	private HomeConnection connection;
	private List<HomeConnectChannel> channels; 
	private RecordsReceivedListener listener;
	private HomeConnectApiClient client;
	
	private int connectionCounter;
	private boolean threadActive = false;
	private boolean clientOnline = false;
	
	public HomeConnectListener(HomeConnection connection, HomeConnectApiClient client ,List<HomeConnectChannel> channels, RecordsReceivedListener listener) {
		this.connection = connection;
		this.client = client;
		this.channels = channels;
		this.listener = listener;
		connectionCounter = 0;
	}
	
	@Override
	   public void run() {
		
		threadActive = true;
		
		long samplingTime = System.currentTimeMillis();
		
		Data data;
		
		try {
			for (HomeConnectChannel channel : channels) {
				
				try {
					
					data = client.get(channel.getHomeApplianceId(), channel.getResource(), samplingTime);
					
					putNewData(data.getValue(),channel.getResource().getKey(),samplingTime);
					
				} catch (UnsupportedOperationException e) {
					channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
	                logger.warn("Unable to read resource {}", channel.getResource());
				} catch (InvalidScopeOrIdException e) {
				    channel.setFlag(Flag.DRIVER_ERROR_READ_FAILURE);
				    logger.warn("Wrong scope or haId configured for resource {}", channel.getResource());
			    }
				
			}
			
			clientOnline = true;
			
		} catch(HomeConnectException e) {
			
			if (e.getMessage().contains("HomeAppliance is offline")){
				
				putApplianceDisconnected(samplingTime);
			}
			else if (e.getMessage().contains("429")){
				
				putTooManyRequests(samplingTime, e.getMessage());
				
			}
			else {
				client.eventClient.dispose();
				listener.connectionInterrupted(HomeConnectDriver.ID, connection);
				threadActive = false;
				clientOnline = false;
			}
		}
		
		while(threadActive) {
		
			if (connectionCounter >= 6) {
				client.eventClient.dispose();
				listener.connectionInterrupted(HomeConnectDriver.ID, connection);
				threadActive =false;
				clientOnline = false;
	 		}
			
			++connectionCounter;
			
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onEvent(Event event) {
		System.out.println(event.toString());
		long samplingTime = System.currentTimeMillis();
		connectionCounter = 0;
		event.getType();
		if(event.getType() == EventType.CONNECTED) {
			clientOnline = true;
		}
		else if (event.getType() == EventType.DISCONNECTED) {
			putApplianceDisconnected(samplingTime);
		}
		
		else if(event.getType() != EventType.KEEP_ALIVE) {
			
			try {
				
				putNewData(event.getValue(),event.getKey(),samplingTime);
				
			}catch (Exception e) {
				System.out.println(event.toString());
			}
		
		}
		
	}
	
	private void putNewData(String value, String key, long timestamp) {
		
		List<ChannelRecordContainer> eventContainers = new ArrayList<ChannelRecordContainer>(); 
		
		if (key.contains(Constants.FRIDGE_MEASURED_TEMPERATURE) ||key.contains(Constants.FREEZER_MEASURED_TEMPERATURE)) {
 			
			value = String.valueOf(Double.parseDouble(value)/8);
		}
		
		Data data = new Data(key, value, null);
		
		for(HomeConnectChannel channel : channels){
			
			if(channel.getResource().getKey().contains(key)) {
			
				channel.setData(data,timestamp);
				
				eventContainers.add((ChannelRecordContainer) channel.getTaskContainer());
				
			}
		}
		
		if (eventContainers.size() > 0) {
			
			listener.newRecords(eventContainers);
		}
		
	}
	
	private void putApplianceDisconnected(long timestamp) {
		
		for (HomeConnectChannel channel : channels) {
			
			try {
		
				if (channel.getResource().getValueType() == 0){
					putNewData("HomeAppliance is offline",channel.getResource().getKey(),timestamp);
				}
				
			} catch (UnsupportedOperationException e1) {
				channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
                logger.warn("Unable to read resource {}", channel.getResource());
			}
		}
		
		clientOnline = false;
	}
	
	private void putTooManyRequests(long timestamp, String message) {
		
		for (HomeConnectChannel channel : channels) {
			
			try {
		
				if (channel.getResource().getValueType() == 0){
					putNewData("Too many requests in a given amount of time",channel.getResource().getKey(),timestamp);
				}
				
			} catch (UnsupportedOperationException e1) {
				channel.setFlag(Flag.ACCESS_METHOD_NOT_SUPPORTED);
                logger.warn("Unable to read resource {}", channel.getResource());
			}
		}
		
		clientOnline = false;
	}
	
}
