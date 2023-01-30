package com.homeconnect.client.listener;

import com.homeconnect.client.model.Event;

public class FridgeEventListener implements HomeConnectEventListener{

	@Override
	public void onEvent(Event event) {
		
		//Ausgabe der Event Daten
		System.out.println(event.toString());
		
	}
	
}
