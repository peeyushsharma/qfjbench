package org.qfjbench.sample.app;

import quickfix.DefaultSessionFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

public class Client {
	
	public static void main(String[] args) throws Exception {
		SessionSettings settings = new SessionSettings("client.cfg");
		NoOpApp app = new NoOpApp();
		LogFactory logFactory = new ScreenLogFactory(false,false,true);
		MessageStoreFactory msf = new MemoryStoreFactory();
		DefaultSessionFactory dsf = new DefaultSessionFactory(app,msf,logFactory);
		
		ThreadedSocketInitiator initiator = new ThreadedSocketInitiator(dsf, settings);
		
		initiator.start();
		
		Message nos = getMessage();
		
		do{
			Thread.sleep(100);
		}while(app.sessionId==null);
		
		for (int i=0;i<Integer.MAX_VALUE;i++) {
			Thread.sleep(5);
			app.send(nos);
		}
		
		
		Thread.currentThread().join();
	}
	
	static Message getMessage(){
		NewOrderSingle nos = new NewOrderSingle();
		nos.set(new ClOrdID("1000"));
		nos.set(new OrderQty(1000));	
		nos.set(new Symbol("EUR/USD"));
		nos.set(new Side(Side.BUY));
		nos.set(new TransactTime());
		nos.set(new OrdType(OrdType.MARKET));
		return nos;
	}
	

}
