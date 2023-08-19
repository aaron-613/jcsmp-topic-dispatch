package dev.solace.aaron.topic;

public class Sub implements CharSequence {
	
	public final String sub;

	/**
	 * <p>Builds a convenience Solace topic subscription object, can be used for topic dispatch.</p>
	 * @param sub a topic subscription, something like <code>a/b/c</code> or <code>a/b/*</code> or <code>a/b/&gt;</code>
	 * @throws IllegalArgumentException if the subscription is not well-formed. See {@link TopicUtil#validateSub(String)}
	 */
	public Sub(String sub) {
		this.sub = sub;
		if (!TopicUtil.validateSub(this.sub)) {
			throw new IllegalArgumentException("Malformed subscription: " + this.sub);
		}
	}

	/** Convenience method for {@link TopicUtil#topicMatches(String,String)} */
	public boolean matches(String topic) {
		return TopicUtil.topicMatches(topic, sub);
	}
	
	@Override
	public String toString() {
		return sub;
	}
	
	/** 
	 * <p><b>Can be used against other Sub objects, or String object as well.</b></p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof Sub) {
			Sub otherSub = (Sub)o;
			return sub.equals(otherSub.sub);
		} else if (o instanceof String) {
			return sub.equals((String)o);
		} else return false;
	}
	
	@Override
	public int hashCode() {
		return sub.hashCode();
	}

	@Override
	public int length() {
		return sub.length();
	}

	@Override
	public char charAt(int index) {
		return sub.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return sub.subSequence(start, end);
	}

}
