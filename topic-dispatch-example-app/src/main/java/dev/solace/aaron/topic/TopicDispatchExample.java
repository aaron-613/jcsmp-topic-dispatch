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
import java.util.Collections;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.XMLMessageConsumer;

/** This is a more detailed subscriber sample. */
public class TopicDispatchExample {

    private static final String SAMPLE_NAME = TopicDispatchExample.class.getSimpleName();
    private static final String API = "JCSMP";

    private static volatile boolean isShutdown = false;          // are we done yet?

    /** the main method. */
    public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
        if (args.length < 3) {  // Check command line arguments
            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> [password]%n%n", SAMPLE_NAME);
            System.exit(-1);
        }
        System.out.println(API + " " + SAMPLE_NAME + " initializing...");

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
        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
                System.out.printf("### Received a Session event: %s%n", event);
            }
        });
        session.connect();  // connect to the broker

        // Anonymous inner-class for MessageListener, this demonstrates the async threaded message callback
        MessageListenerWithTopicDispatch specialListener = new SpecialListener();
        specialListener.addCallback("a/b/>", new MessageListenerWithUserData("a/b/>") {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for %s received: %n%s", userData.toString(), msg.dump());
			}
			
			@Override
			public void onException(JCSMPException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		});
        specialListener.addCallback("a/*/c", new MessageListenerWithUserData("2nd callback a/*/c") {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for %s received: %n%s", userData.toString(), msg.dump());
			}
			
			@Override
			public void onException(JCSMPException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		});
        specialListener.addCallback("*/b/c", new MessageListenerWithUserData(Collections.singletonMap("key", "value")) {
			
			@Override
			public void onReceive(BytesXMLMessage msg) {
				System.out.printf("Callback for %s received: %n%s", userData.toString(), msg.dump());
			}
			
			@Override
			public void onException(JCSMPException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		});
        
        
        
        
        final XMLMessageConsumer consumer = session.getMessageConsumer(specialListener);

        session.addSubscription(JCSMPFactory.onlyInstance().createTopic(">"));
        // add more subscriptions here if you want
        consumer.start();
        System.out.println(API + " " + SAMPLE_NAME + " connected, and running. Press [ENTER] to quit.");
        while (System.in.available() == 0 && !isShutdown) {
            Thread.sleep(100);  // wait 1 second
        }
        isShutdown = true;
        session.closeSession();  // will also close consumer object
        System.out.println("Main thread quitting.");
    }
    
    
    
    
    
    private static class SpecialListener extends MessageListenerWithTopicDispatch {

		public SpecialListener() {
			super(Mode.DEDUPE);
		}

		@Override
		public void onReceiveDefault(BytesXMLMessage message) {
			System.out.println("Default message callback! Not handled with special calback");
			System.out.println(message.dump());
		}

		@Override
		public void onExceptionDefault(JCSMPException e) {  // uh-oh!
            System.out.printf("### MessageListener's onException(): %s%n",e);
            if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed
                isShutdown = true;  // let's quit; or, could initiate a new connection attempt
            }
		}
    	
    }
    
    

    

    
}
