/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.app.simpledemo;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {})
public final class SimpleDemoApp extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDemoApp.class);

    // ChannelIDs, see conf/channel.xml
    private static final String ID_GRID_IMPORT = "grid_import_power";
    private static final String ID_WASHER_ACTIVE_PROGRAM = "Washer_active_program";
    private static final String ID_WASHER_TIME_DURATION = "Washer_time_duration";
    private static final String ID_WASHER_END_TIME = "Washer_end_time";

    private boolean programActive;
    private boolean onRequest;
    private boolean startedOnRequest;
    private int washerTimeDuration;
	private long washerEndTime;
	
    private volatile boolean deactivatedSignal;

    // With the dataAccessService you can access to your measured and control data of your devices.
    @Reference
    private DataAccessService dataAccessService;

    // Channel for accessing data of a channel.
    private Channel chGridImport;
    private Channel chWasherActiveProgram;
    private Channel chWasherTimeDuration;
    private Channel chWasherEndTime; 

    /**
     * Every app needs one activate method. Is is called at begin. Here you can configure all you need at start of your
     * app. The Activate method can block the start of your OpenMUC, f.e. if you use Thread.sleep().
     */
    @Activate
    private void activate() {
        logger.info("Activating Demo App");
        setName("OpenMUC Simple Demo App");
        start();
    }

    /**
     * Every app needs one deactivate method. It handles the shutdown of your app e.g. closing open streams.
     */
    @Deactivate
    private void deactivate() {
        logger.info("Deactivating Demo App");
        deactivatedSignal = true;

        interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * application logic
     */
    @Override
    public void run() {

        logger.info("Demo App started running...");

        if (deactivatedSignal) {
            logger.info("DemoApp thread interrupted: will stop");
            return;
        }

        initializeChannels();
      
        while (!deactivatedSignal) {
        	programListener();
        	checkStartOfProgram();
        	sleepMs(5000);
        }
    }

    /**
     * Initialize channel objects
     */
    private void initializeChannels() {
        
        chGridImport = dataAccessService.getChannel(ID_GRID_IMPORT);
        chWasherActiveProgram = dataAccessService.getChannel(ID_WASHER_ACTIVE_PROGRAM);
        chWasherEndTime = dataAccessService.getChannel(ID_WASHER_END_TIME);
        chWasherTimeDuration= dataAccessService.getChannel(ID_WASHER_TIME_DURATION);
    } 
    
    /**
     * Listening for active program, save values and start request
     */
    private void programListener() {
    		if (chWasherActiveProgram.getLatestRecord().getValue() != null 
    				&& chWasherActiveProgram.getLatestRecord().getValue().asBoolean() != programActive) {
    			
    			programActive = chWasherActiveProgram.getLatestRecord().getValue().asBoolean();		
    			
    			//Checks if program is started without a running request
    			if(programActive && !onRequest) {
    				
    				//Indicates request
    				onRequest = true;
    			    washerTimeDuration = chWasherTimeDuration.getLatestRecord().getValue().asInt();
    			    washerEndTime = (System.currentTimeMillis() / 1000) + chWasherEndTime.getLatestRecord().getValue().asInt();
    			    
    			    //Stop program for remote control
        		    chWasherActiveProgram.write(new BooleanValue(false));
        		    programActive = false;
        		    
        		    logger.info("Request for remote start.");
        		//Closes request after program has been started    
    			} else if(programActive && onRequest && startedOnRequest) {
    				onRequest = false;
    				startedOnRequest = false; 
    				logger.info("Request closed after program has been started.");
    			//Resets everything if program has been changed
    			} else if(programActive && onRequest) {
    				onRequest = false;
    				programActive = false;
    				logger.info("Program changed: Reset all flags.");
    			} else {		
    			}
    		}
    }
    
    /**
     * Starts program, when conditions are fulfilled 
     */
    private void checkStartOfProgram() {
    	
    	//Current time in unix timestamp
    	long actualTime = System.currentTimeMillis() / 1000;

    	//Checks if request is on and program isn't already running
    	if(onRequest && !programActive) {
    		
    		//Starts program 60s before last possibility to fulfill endtime condition
    		if((actualTime + washerTimeDuration) > (washerEndTime - 60)) {
    			startedOnRequest = true;
        		chWasherActiveProgram.write(new BooleanValue(true));
                logger.info("Washer started.");
        	}
    		
    	    //Starts program if PV power is over threshold 
    		else if(chGridImport.getLatestRecord().getValue().asInt() < 150000) {
    			startedOnRequest = true;
        		chWasherActiveProgram.write(new BooleanValue(true));
        		logger.info("Washer started.");
        	} else {
        		
        	}
    	}	
    }

    private void sleepMs(long timeInMs) {
        try {
            Thread.sleep(timeInMs);
        } catch (InterruptedException e) {
        }
    }
}
