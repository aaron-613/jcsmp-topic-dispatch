package com.solace.labs.topic;

public abstract class MessageListenerWithUserData implements DispatchMessageListener {
	
	protected final Object userData;
	
	public MessageListenerWithUserData(Object userData) {
		this.userData = userData;
	}
	
	public Object getUserData() {
		return userData;
	}
}
