package dev.solace.aaron.topic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;

public abstract class MessageListenerWithTopicDispatchOld implements XMLMessageListener {

	public enum Mode {
		/** If there are overlapping subscriptions for the same callback, the callback will only be called once */
		DEDUPE,
		/** Even if there are overlapping subscriptions for the same callback, each callback will be called */
		CALL_EACH,
		;
	}
	
	private Map<String, Set<XMLMessageListener>> callbacks = new HashMap<>();
	protected Mode mode;
	
	public MessageListenerWithTopicDispatchOld(Mode mode) {
		this.mode = mode;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}
	
	public void addCallback(String sub, XMLMessageListener callback) {
		if (!TopicUtil.validateSub(sub)) {
			throw new IllegalArgumentException("Malformed subscription: " + sub);
		}
		if (!callbacks.containsKey(sub)) {
			callbacks.put(sub, new LinkedHashSet<>());
		}
		callbacks.get(sub).add(callback);
	}
	
	public abstract void onReceiveDefault(BytesXMLMessage message);
	public abstract void onExceptionDefault(JCSMPException e);
	
    @Override
    public void onReceive(BytesXMLMessage message) {
    	String topic = message.getDestination().getName();
		Set<XMLMessageListener> calledCallbacks = new HashSet<>();
//		Set<XMLMessageListener> erroredCallbacks = new HashSet<>();
		for (String sub : callbacks.keySet()) {
			if (TopicUtil.topicMatches(topic, sub)) {  // topic match!
				for (Iterator<XMLMessageListener> it = callbacks.get(sub).iterator(); it.hasNext(); ) {
					XMLMessageListener callback = it.next();
					if (!calledCallbacks.contains(callback)) {  // haven't called this guy yet
						if (mode == Mode.DEDUPE) calledCallbacks.add(callback);  // only if we're in de-dupe mode, otherwise call each one we find
						try {
							callback.onReceive(message);
						} catch (Exception e) {
							it.remove();
							callback.onException(new JCSMPException("Caught exception on custom callback for sub " + sub +", unregistering", e));
						}
					}					
				}
/*				for (XMLMessageListener callback : callbacks.get(sub)) {
					if (!calledCallbacks.contains(callback)) {  // haven't called this guy yet
						if (mode == Mode.DEDUPE) calledCallbacks.add(callback);  // only if we're in de-dupe mode, otherwise call each one we find
						try {
							callback.onReceive(message);
						} catch (Exception e) {
							callback.onException(new JCSMPException("Caught exception on custom callback, unregistering", e));
							// figure out how to unregister here
							erroredCallbacks.add(callback);
						}
					}
				}
*/			}
		}
/*		if (!erroredCallbacks.isEmpty()) {
    		// do we need to unregister anything?
    		for (XMLMessageListener callback : erroredCallbacks) {
    			for (Set<XMLMessageListener> toCheck : callbacks.values()) {
    				toCheck.remove(callback);  // just try to remove, brute force
    			}
    		}
    		// check if any subs have no callbacks left
    		for (Iterator<Map.Entry<String, Set<XMLMessageListener>>> it = callbacks.entrySet().iterator(); it.hasNext(); ) {
    			if (it.next().getValue().isEmpty()) {
    				it.remove();
    			}
    		}
		}
*/		// check if any subs have no callbacks left
		for (Iterator<Map.Entry<String, Set<XMLMessageListener>>> it = callbacks.entrySet().iterator(); it.hasNext(); ) {
			if (it.next().getValue().isEmpty()) {
				it.remove();
			}
		}
    }

    @Override
    public void onException(JCSMPException e) {  // uh oh!
    	onExceptionDefault(e);
    }
}
