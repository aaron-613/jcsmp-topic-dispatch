package dev.solace.aaron.topic;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;

public interface DefaultMessageListener {
	public void onReceiveDefault(BytesXMLMessage msg);
	public void onException(JCSMPException e);
}
