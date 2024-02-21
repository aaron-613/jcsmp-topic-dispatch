package com.solace.labs.topic;

import com.solacesystems.jcsmp.BytesXMLMessage;

public interface DispatchMessageListener {
	public void onReceive(BytesXMLMessage msg);
//	public void onUncaughtException(String message, Exception e);
	public void onUncaughtException(TopicDispatchException e);
}
