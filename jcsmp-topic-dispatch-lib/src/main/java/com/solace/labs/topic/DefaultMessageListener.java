package com.solace.labs.topic;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;

public interface DefaultMessageListener {
	public void onReceiveDefault(BytesXMLMessage msg);
	public void onException(JCSMPException e);
}
