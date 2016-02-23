package org.qfjbench.sample.app;

import quickfix.ApplicationAdapter;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;

public class NoOpApp extends ApplicationAdapter {
	
	long recv = 0;
	long sent = 0;
	
	SessionID sessionId;
	
    public void onLogon(SessionID sessionId) {
    	this.sessionId = sessionId;
    }
	
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    	recv++;
    	if( recv % 1000 == 0 ){
    		System.out.println("working");
    	}
    }
    
    public void toApp(Message message, SessionID sessionId) {
    	sent++;
    }
    
    public void send(Message message){
    	try {
			Session.sendToTarget(message, sessionId);
		} catch (SessionNotFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
