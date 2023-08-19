package dev.solace.aaron.topic;

import com.solacesystems.jcsmp.XMLMessageListener;

public abstract class MessageListenerWithUserData implements XMLMessageListener {
	
	protected final Object userData;
	
	public MessageListenerWithUserData(Object userData) {
		this.userData = userData;
	}
	
	public Object getUserData() {
		return userData;
	}
}