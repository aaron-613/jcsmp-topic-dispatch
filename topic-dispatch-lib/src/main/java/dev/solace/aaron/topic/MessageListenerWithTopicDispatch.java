package dev.solace.aaron.topic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;

public final class MessageListenerWithTopicDispatch implements XMLMessageListener {

	public enum Mode {
		/** If there are overlapping subscriptions for the exact same callback, the callback will only be called once */
		DEDUPE,
		/** Even if there are overlapping subscriptions for the exact same callback, each callback will be called */
		CALL_EACH,
		;
	}
	
	private static class Helper {
		
	}
	
	private Map<Sub, Set<DispatchMessageListener>> callbacks = new HashMap<>();  // for each subscription, a Set of callbacks
	private ReentrantLock lock = new ReentrantLock();
	private AtomicBoolean lock2 = new AtomicBoolean(false);
	private final Mode mode;
	private DefaultMessageListener defaultMessageListener;
	
	// these variables are reused on each invocation of onReceive()
	private Set<DispatchMessageListener> calledCallbacks = new HashSet<>();
	boolean onReceiveMatch = false;
	boolean onReceiveException = false;
	
	
/*	public MessageListenerWithTopicDispatch(Mode mode) {
		this.mode = mode;
	}
*/
	public MessageListenerWithTopicDispatch(Mode mode, DefaultMessageListener defaultMessageListener) {
		this.mode = mode;
		if (this.mode == null) {
			throw new NullPointerException("Must specify a non-null Mode for the Topic Dispatcher");
		}
		this.defaultMessageListener = defaultMessageListener;
		if (this.defaultMessageListener == null) {
			throw new NullPointerException("Must specify a non-null XMLMessageListener for the default message callback");
		}
	}

	public Mode getMode() {
		return mode;
	}
	
	// possible threading issues around this
	public void registerCallback(Sub sub, DispatchMessageListener callback, boolean stillInvokeDefaulListener) {
		if (sub == null) throw new NullPointerException("Sub is null");
		if (callback == null) throw new NullPointerException("Callback is null");
		lock.lock();
		while (lock2.compareAndSet(false, true)) { } // busy wait
		try {
			if (!callbacks.containsKey(sub)) {
				callbacks.put(sub, new HashSet<>(4));
			}
			callbacks.get(sub).add(callback);
		} finally {
			lock.unlock();
			lock2.set(false);
		}
	}
	
	// possible threading issues around this
	public boolean unregisterCallback(Sub sub, XMLMessageListener callback) {
		if (sub == null) throw new NullPointerException("Sub is null");
		if (callback == null) throw new NullPointerException("Callback is null");
		lock.lock();
		while (lock2.compareAndSet(false, true)) { } // busy wait
		try {
			if (!callbacks.containsKey(sub)) return false;
			boolean existed = callbacks.get(sub).remove(callback);
			if (existed) {
				if (callbacks.get(sub).isEmpty()) {
					callbacks.remove(sub);
				}
			}
			return existed;
		} finally {
			lock.unlock();
			lock2.set(false);
		}
	}
	
/*	@Override
	public void onReceiveDefault(BytesXMLMessage message) {
		if (defaultMessageListener == null) throw new AssertionError("Did not specify default listener");
		defaultMessageListener.onReceive(message);
	}
	
	@Override
	public void onExceptionDefault(JCSMPException e) {
		if (defaultMessageListener == null) throw new AssertionError("Did not specify default listener");
		defaultMessageListener.onException(e);
	}
*/
//	@Override
//	public abstract void onReceiveDefault(BytesXMLMessage message);
//	
//	@Override
//	public abstract void onExceptionDefault(JCSMPException e);

    @Override
    public void onReceive(BytesXMLMessage message) {
    	String topic = message.getDestination().getName();
    	// reset my vars
		calledCallbacks.clear();
		onReceiveMatch = false;
		onReceiveException = false;
		lock.lock();
		while (lock2.compareAndSet(false, true)) { } // busy wait
		try {
			for (Sub sub : callbacks.keySet()) {
				if (sub.matches(topic)) {  // topic match!
					onReceiveMatch = true;
					for (Iterator<DispatchMessageListener> it = callbacks.get(sub).iterator(); it.hasNext(); ) {
						DispatchMessageListener callback = it.next();
						if (!calledCallbacks.contains(callback)) {  // haven't called this guy yet
							if (mode == Mode.DEDUPE) calledCallbacks.add(callback);  // only if we're in de-dupe mode, otherwise call each one we find
							try {
								callback.onReceive(message);
							} catch (Exception e) {
								onReceiveException = true;
								it.remove();
								try {
									callback.onUncaughtException("Uncaught exception from custom callback for sub " + sub +", unregistering", e);
								} catch (Exception e2) {
									// ignore, we're unregistering
								}
							}
						}
					}
				}
			}
			if (onReceiveException) {
				// check if any subs have no callbacks left due to unregistering...
				for (Iterator<Map.Entry<Sub, Set<DispatchMessageListener>>> it = callbacks.entrySet().iterator(); it.hasNext(); ) {
					if (it.next().getValue().isEmpty()) {
						it.remove();
					}
				}
			}
			if (!onReceiveMatch) {  // didn't match any custom callbacks
				try {
					defaultMessageListener.onReceiveDefault(message);
				} catch (Exception e) {
					defaultMessageListener.onException(new JCSMPException("Uncaught exception from onReceiveDefault()", e));
				}
			}
		} finally {
			lock.unlock();
			lock2.set(false);
		}
    }

    @Override
    public void onException(JCSMPException e) {  // uh oh!
    	defaultMessageListener.onException(e);
    }
}
