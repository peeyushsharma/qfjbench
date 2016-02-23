package org.qfjbench.sample.app;

import quickfix.DefaultSessionFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketAcceptor;
import quickfix.ThreadedSocketInitiator;

public class Server {
	
	public static void main(String[] args) throws Exception {
		SessionSettings settings = new SessionSettings("server.cfg");
		NoOpApp app = new NoOpApp();
		LogFactory logFactory = new ScreenLogFactory(false,false,true);
		MessageStoreFactory msf = new MemoryStoreFactory();
		DefaultSessionFactory dsf = new DefaultSessionFactory(app,msf,logFactory);
		
		ThreadedSocketAcceptor initiator = new ThreadedSocketAcceptor(dsf, settings);
		
		initiator.start();
		
		Thread.currentThread().join();
	}
	
	

}
