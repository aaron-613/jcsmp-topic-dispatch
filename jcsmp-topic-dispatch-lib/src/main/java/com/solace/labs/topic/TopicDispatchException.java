package com.solace.labs.topic;

import com.solacesystems.jcsmp.JCSMPException;

public class TopicDispatchException extends JCSMPException {

	private static final long serialVersionUID = 1L;

	public TopicDispatchException(String message, Throwable cause) {
		super(message, cause);
	}

}
