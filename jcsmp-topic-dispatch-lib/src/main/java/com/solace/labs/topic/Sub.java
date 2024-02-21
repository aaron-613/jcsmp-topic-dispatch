package com.solace.labs.topic;

import java.util.regex.Pattern;

/**
 * This class represents a topic subscription, and includes fancy regex (regular expression) parsing
 * of the topic string to replace the '*' and '&gt;' SMF wildcards into regex quantifiers
 */
public class Sub implements CharSequence {
	
	public final String topicSubscription;
	public final String regex;
	public final Pattern pattern;

	/**
	 * <p>Builds a convenience Solace topic subscription object, can be used for topic dispatch,
	 * as well as DIY regex pattern matching.</p>
	 * @param topicSubscription a topic subscription, something like <code>a/b/c</code> or <code>a/b/*</code> or <code>a/b/&gt;</code>
	 * @throws IllegalArgumentException if the subscription is not well-formed. See {@link TopicUtils#validateSubLinear(String)}
	 */
	public Sub(String topicSubscription) {
		this.topicSubscription = topicSubscription;
		if (!TopicUtils.validateSubLinear(this.topicSubscription)) {
			throw new IllegalArgumentException("Malformed subscription: " + this.topicSubscription);
		}
		regex = TopicUtils.buildSubRegexNoChecks(this.topicSubscription);
		pattern = Pattern.compile(regex);
	}

	/*
	 * {@link  dev.solace.aaron.useful.dispatch.Sub#Sub(String)  Convenience method for Sub constructor}
	 */
//	public static Sub makeTopicSubscription(String topicSubscription) {
//		return new Sub(topicSubscription);
//	}
	
	
	/**
	 * Convenience method for {@link TopicUtils#topicMatches(String,String)}.
	 * Does not use regex matching, just the linear version.
	 * @param topic: the topic to match against this subscription
	 * @return true if there is a match; false otherwise
	 */
	public boolean matches(String topic) {
		return TopicUtils.topicMatches(topic, topicSubscription);
	}
	
	@Override
	public String toString() {
		return topicSubscription;
	}
	
	/**
	 * Exactly the same as the original topic subscription, but a convenience method
	 * to replace the subscription with MQTT 0-or-more levels wildcard '#' or one that ends with '/#'
	 * with the SMF equivalent, which is ASCII char 0x03.
	 * 
	 * @see <a href="https://solace.community/discussion/618/magic-hidden-solace-topic-subscription-wildcards">Hidden SMF wildcards</a>
	 * @return
	 */
	public String toSolaceSubscription() {
		if (topicSubscription.equals("#")) {
			return "\03";
		} else if (topicSubscription.endsWith("/#")) {
			return topicSubscription.substring(0, topicSubscription.length()-2) + "\03";
		}
		return topicSubscription;
	}
	
	/** 
	 * <p><b>Can be used against other Sub objects, or String objects as well.</b></p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof Sub) {
			Sub otherSub = (Sub)o;
			return topicSubscription.equals(otherSub.topicSubscription);
		} else if (o instanceof String) {
			return topicSubscription.equals(o) || toSolaceSubscription().equals(o);
		} else return false;
	}
	
	@Override
	public int hashCode() {
		return topicSubscription.hashCode();
	}

	// needed for implementing the CharSequence interface
	@Override
	public int length() {
		return topicSubscription.length();
	}

	// needed for implementing the CharSequence interface
	@Override
	public char charAt(int index) {
		return topicSubscription.charAt(index);
	}

	// needed for implementing the CharSequence interface
	@Override
	public CharSequence subSequence(int start, int end) {
		return topicSubscription.subSequence(start, end);
	}

}
