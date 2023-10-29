package dev.solace.aaron.topic;

import com.solacesystems.jcsmp.BytesXMLMessage;

public interface DispatchMessageListener {
	public void onReceive(BytesXMLMessage msg);
	public void onUncaughtException(String message, Exception e);
}
