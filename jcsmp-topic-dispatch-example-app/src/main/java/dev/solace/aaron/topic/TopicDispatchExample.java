/*
 * Copyright 2021-2022 Solace Corporation. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package dev.solace.aaron.topic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import com.solace.labs.topic.DefaultMessageListener;
import com.solace.labs.topic.MessageListenerWithTopicDispatch;
import com.solace.labs.topic.MessageListenerWithUserData;
import com.solace.labs.topic.Sub;
import com.solace.labs.topic.TopicDispatchException;
import com.solace.labs.topic.MessageListenerWithTopicDispatch.Mode;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageProducer;

/** This is a more detailed subscriber sample. */
public class TopicDispatchExample {

    private static final String SAMPLE_NAME = TopicDispatchExample.class.getSimpleName();
    private static final String API = "JCSMP";

    private static volatile boolean isShutdown = false;          // are we done yet?

    
    
    JCSMPSession session;
    XMLMessageConsumer consumer;
    XMLMessageProducer producer;

    
    public TopicDispatchExample(String... args) throws InvalidPropertiesException {
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);      // client-username
        if (args.length > 3) {
            properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        }
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // subscribe Direct subs after reconnect
        JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
        channelProps.setReconnectRetries(20);      // recommended settings
        channelProps.setConnectRetriesPerHost(5);  // recommended settings
        // https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
                System.out.printf("### Received a Session event: %s%n", event);
            }
        });    	
    }
    
    private void initConsumer() throws JCSMPException {
        // Anonymous inner-class for MessageListener, this demonstrates the async threaded message callback
    	MessageListenerWithTopicDispatch dispatchListener = new MessageListenerWithTopicDispatch(Mode.DEDUPE, new DefaultMessageListener() {

			@Override
			public void onReceiveDefault(BytesXMLMessage msg) {
				System.out.printf("DEFAULT Callback: %s%n", printMsg(msg));
			}

			@Override
			public void onException(JCSMPException e) {
				System.out.printf("DEFAULT Callback Exception!%n");
				e.printPartialStackTrace(null);
			}
		});
        consumer = session.getMessageConsumer(dispatchListener);
        dispatchListener.registerCallback(new Sub("a*/>"), new MessageListenerWithUserData("All the a's") {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for userdata '%s' received: %s%n", userData.toString(), printMsg(msg));
			}
			
			@Override
			public void onUncaughtException(TopicDispatchException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}

//			@Override
//			public void onUncaughtException(String message, Exception e) {
//				// TODO Auto-generated method stub
//			}
			
		});
        
        dispatchListener.registerCallback(new Sub("b*/>"), new MessageListenerWithUserData("Only b's") {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for userdata '%s' received: %s%n", userData.toString(), printMsg(msg));
			}
			
			@Override
			public void onUncaughtException(TopicDispatchException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		});
        dispatchListener.registerCallback(new Sub("*/*/*"), new MessageListenerWithUserData(Collections.singletonMap("key", "subs with exactly 3 levels")) {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for userdata '%s' received: %s%n", userData.toString(), printMsg(msg));
			}
			
			@Override
			public void onUncaughtException(TopicDispatchException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		});
        session.addSubscription(JCSMPFactory.onlyInstance().createTopic(">"));  // EVERYTHING!!
        // add more subscriptions here if you want
        consumer.start();
    }
    
    public static String printMsg(BytesXMLMessage msg) {
    	StringBuilder sb = new StringBuilder();
    	sb.append(msg.getDestination().getName()).append(": ");
    	ByteBuffer bb = msg.getAttachmentByteBuffer();
    	sb.append(new String(bb.array()));
    	return sb.toString();
    }
    
    
    
    /** the main method. */
    public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
        if (args.length < 3) {  // Check command line arguments
            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> [password]%n%n", SAMPLE_NAME);
            System.exit(-1);
        }
        System.out.println(API + " " + SAMPLE_NAME + " initializing...");

        TopicDispatchExample app = new TopicDispatchExample(args);
        app.session.connect();
        app.initConsumer();

        System.out.println(API + " " + SAMPLE_NAME + " connected, and running. Press [ENTER] to quit.");
        while (System.in.available() == 0 && !isShutdown) {
            Thread.sleep(100);  // sleep for 100 ms, waiting to see if shutdown or [ENTER] pressed
        }
        isShutdown = true;
        app.session.closeSession();  // will also close consumer object
        System.out.println("Main thread quitting.");
    }
    
    
}
