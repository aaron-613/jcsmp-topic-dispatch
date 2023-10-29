package dev.solace.aaron.topic;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;

public final class MessageListenerWithTopicDispatch2 /* extends MessageListenerWithTopicDispatch */ {
	
/*	protected XMLMessageListener defaultMessageListener;

	public MessageListenerWithTopicDispatch2(Mode mode, XMLMessageListener defaultMessageListner) {
		super(mode);
		this.defaultMessageListener = defaultMessageListner;
		if (this.defaultMessageListener == null) {
			throw new NullPointerException("Must specify a non-null XMLMessageListener for the default message callback");
		}
	}

	@Override
	public void onReceiveDefault(BytesXMLMessage message) {
		defaultMessageListener.onReceive(message);
	}
	
	@Override
	public void onExceptionDefault(JCSMPException e) {
		defaultMessageListener.onException(e);
	}*/
}
