package org.qfjbench;

import java.io.IOException;

import quickfix.Application;
import quickfix.DefaultSessionFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.InvalidMessage;
import quickfix.MemoryStore;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SessionState;
import quickfix.SystemTime;
import quickfix.UnsupportedMessageType;
import quickfix.Message.Header;
import quickfix.field.BeginString;
import quickfix.field.LastMsgSeqNumProcessed;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.SenderCompID;
import quickfix.field.SenderLocationID;
import quickfix.field.SenderSubID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;
import quickfix.field.TargetLocationID;
import quickfix.field.TargetSubID;

public class FIXUtil {
	
	Session session = null;
	
	Application app = new TestApp();
	
	SessionState state;
	
	boolean enableLastMsgSeqNumProcessed = false;
	boolean persistMessages = false;
	boolean millisecondsInTimeStamp = true;
	
	final SessionID sessionID = new SessionID(FixVersions.BEGINSTRING_FIX44, "SENDER", "TARGET");
	
	final Object responderLock = new Object();
	
	SessionSettings getSessionSettings(){
		 SessionSettings settings = new SessionSettings();
		 settings.setString(SessionFactory.SETTING_CONNECTION_TYPE, SessionFactory.ACCEPTOR_CONNECTION_TYPE);
		 settings.setString(Session.SETTING_START_TIME, "00:00:01");
		 settings.setString(Session.SETTING_END_TIME, "23:59:59");
		 return settings;
	}

	void init(){
		
		DefaultSessionFactory sessionFactory = new DefaultSessionFactory(app, new MemoryStoreFactory(), null);

		 
		 SessionSettings settings =  getSessionSettings();
		
		try {
			session = sessionFactory.create(sessionID, settings);
			state = new SessionState(session, null, 60, false, new MemoryStore(), Session.DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER);
			state.setLogonReceived(true);
			state.setLogonSent(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println(session);
	}
	
	void sendMessage(Message message, int num){
		// sequence number must be locked until application
        // callback returns since it may be effectively rolled
        // back if the callback fails.
        state.lockSenderMsgSeqNum();
        try {
            boolean result = false;
            final Message.Header header = message.getHeader();
            final String msgType = header.getString(MsgType.FIELD);

            initializeHeader(header);

            if (num > 0) {
                header.setInt(MsgSeqNum.FIELD, num);
            }

            if (enableLastMsgSeqNumProcessed) {
                if (!header.isSetField(LastMsgSeqNumProcessed.FIELD)) {
                    header.setInt(LastMsgSeqNumProcessed.FIELD, getExpectedTargetNum() - 1);
                }
            }

            String messageString;

            if (message.isAdmin()) {
                try {
                	app.toAdmin(message, sessionID);
                } catch (final Throwable t) {
                	t.printStackTrace();
                }

                if (msgType.equals(MsgType.LOGON)) {
                    if (!state.isResetReceived()) {
                        boolean resetSeqNumFlag = false;
                        if (message.isSetField(ResetSeqNumFlag.FIELD)) {
                            resetSeqNumFlag = message.getBoolean(ResetSeqNumFlag.FIELD);
                        }
                        if (resetSeqNumFlag) {
                           // resetState();
                            message.getHeader().setInt(MsgSeqNum.FIELD, getExpectedSenderNum());
                        }
                        state.setResetSent(resetSeqNumFlag);
                    }
                }

                messageString = message.toString();
                if (msgType.equals(MsgType.LOGON) || msgType.equals(MsgType.LOGOUT)
                        || msgType.equals(MsgType.RESEND_REQUEST)
                        || msgType.equals(MsgType.SEQUENCE_RESET) || isLoggedOn()) {
                    result = true;
                    send(messageString);
                }
            } else {
                try {
                    app.toApp(message, sessionID);
                } catch (final DoNotSend e) {
                    return;
                } catch (final Throwable t) {
                	t.printStackTrace();
                }
                messageString = message.toString();
                if (isLoggedOn()) {
                    result = true;
                    send(messageString);
                }
            }

            if (num == 0) {
                final int msgSeqNum = header.getInt(MsgSeqNum.FIELD);
                if (persistMessages) {
                    state.set(msgSeqNum, messageString);
                }
                state.incrNextSenderMsgSeqNum();
            }

        } catch (final Exception e) {
        	e.printStackTrace();
        } finally {
            state.unlockSenderMsgSeqNum();
        }
	}
	
	Object s = new Object();
	
	private void send(String messageString) {
		
		Object responder;
		synchronized (responderLock) {
			responder = s;
		}
	}

	private void initializeHeader(Message.Header header) {
        state.setLastSentTime(SystemTime.currentTimeMillis());
        header.setString(BeginString.FIELD, sessionID.getBeginString());
        header.setString(SenderCompID.FIELD, sessionID.getSenderCompID());
        optionallySetID(header, SenderSubID.FIELD, sessionID.getSenderSubID());
        optionallySetID(header, SenderLocationID.FIELD, sessionID.getSenderLocationID());
        header.setString(TargetCompID.FIELD, sessionID.getTargetCompID());
        optionallySetID(header, TargetSubID.FIELD, sessionID.getTargetSubID());
        optionallySetID(header, TargetLocationID.FIELD, sessionID.getTargetLocationID());
        header.setInt(MsgSeqNum.FIELD, getExpectedSenderNum());
        insertSendingTime(header);
    }
	
    private void optionallySetID(Header header, int field, String value) {
        if (!value.equals(SessionID.NOT_SET)) {
            header.setString(field, value);
        }
    }

    private void insertSendingTime(Message.Header header) {
        header.setUtcTimeStamp(SendingTime.FIELD, SystemTime.getDate(), includeMillis());
    }
    
    private boolean includeMillis() {
        return millisecondsInTimeStamp
                && sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX42) >= 0;
    }
	
    public boolean isLoggedOn() {
        return sentLogon() && receivedLogon();
    }
    
    public boolean sentLogon() {
        return state.isLogonSent();
    }

    public boolean receivedLogon() {
        return state.isLogonReceived();
    }
	
    public int getExpectedTargetNum() {
        try {
            return state.getMessageStore().getNextTargetMsgSeqNum();
        } catch (final IOException e) {
        	e.printStackTrace();;
        	return -1;
        }
    }
    
    public int getExpectedSenderNum() {
        try {
            return state.getMessageStore().getNextSenderMsgSeqNum();
        } catch (final IOException e) {
        	e.printStackTrace();
            return -1;
        }
    }
	
	
	Message deserialize(String message){
				
		try {
			return MessageUtils.parse(session, message);
			//System.out.println(m.toString());
		} catch (InvalidMessage e) {
			e.printStackTrace();
		}
		return null;
	}
	
	void serialize(Message m){
		try{
			m.toString();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		FIXUtil parser = new FIXUtil();
		parser.init();
		
		String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001" +
	            "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
		
		Message m = parser.deserialize(messageString);
		
		parser.sendMessage(m, 100);
	}
	
	class TestApp implements Application{

		public void onCreate(SessionID sessionId) {
			// TODO Auto-generated method stub
			
		}

		public void onLogon(SessionID sessionId) {
			// TODO Auto-generated method stub
			
		}

		public void onLogout(SessionID sessionId) {
			// TODO Auto-generated method stub
			
		}

		public void toAdmin(Message message, SessionID sessionId) {
			// TODO Auto-generated method stub
			
		}

		public void fromAdmin(Message message, SessionID sessionId)
				throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
			// TODO Auto-generated method stub
			
		}

		public void toApp(Message message, SessionID sessionId) throws DoNotSend {
			// TODO Auto-generated method stub
			
		}

		public void fromApp(Message message, SessionID sessionId)
				throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
			// TODO Auto-generated method stub
			
		}
		
	}

}
