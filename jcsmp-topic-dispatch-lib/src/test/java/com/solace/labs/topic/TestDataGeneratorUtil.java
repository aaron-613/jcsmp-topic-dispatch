package com.solace.labs.topic;

import java.util.Iterator;
import java.util.Set;

public class TestDataGeneratorUtil {

	private static final char[] REG_CHARS = new char[] {'a', 'b', 'c', 'd', '-', '_'};
//	private static final char[] REG_CHARS = new char[] {'a', 'b', 'c', 'd', '[', ']', '-', '_', '#', '(', ')'};

	private static char[] TOPIC_CHARS = new char[REG_CHARS.length + 1];
	private static char[] SUB_CHARS = new char[REG_CHARS.length + 4];
	static {
		System.arraycopy(REG_CHARS, 0, TOPIC_CHARS, 0, REG_CHARS.length);
		System.arraycopy(new char[] { '/'}, 0, TOPIC_CHARS, REG_CHARS.length, 1);
		System.arraycopy(REG_CHARS, 0, SUB_CHARS, 0, REG_CHARS.length);
		System.arraycopy(new char[] { '/', '*', '*', '>'}, 0, SUB_CHARS, REG_CHARS.length, 4);
	}

	/** Could easily be malformed */
	public static String buildRandomSub(int maxLength) {
		assert maxLength > 0;
		assert maxLength < 250;
		StringBuilder sb = new StringBuilder();
		int length = 1 + (int)(Math.random() * (maxLength-1));
		for (int i=0; i<length; i++) {
			sb.append(SUB_CHARS[(int)(Math.random() * SUB_CHARS.length)]);
		}
		return sb.toString();
	}
	
//	private static final char[] TOPIC_CHARS = new char[] {'a', 'b', 'c', 'd', '/'};

	// won't have any leading or trailing slashes
	public static String buildRandomTopic(int maxLength) {
		assert maxLength > 0;
		assert maxLength < 250;
		StringBuilder sb = new StringBuilder();
		int length = 1 + (int)(Math.random() * (maxLength-1));
		sb.append(TOPIC_CHARS[(int)(Math.random() * REG_CHARS.length)]);
		if (length > 1) {
			for (int i=1; i<length-1; i++) {
				sb.append(TOPIC_CHARS[(int)(Math.random() * TOPIC_CHARS.length)]);
			}
			sb.append(TOPIC_CHARS[(int)(Math.random() * REG_CHARS.length)]);
		}
		return sb.toString();
	}

	/** Destructively modifies the passed-in Set to remove any topics that start or end with /, or have empty-levels // */
	public static void trimMalformedTopics(Set<String> subs) {
		for (Iterator<String> it = subs.iterator(); it.hasNext(); ) {
			String topic = it.next();
			if (topic.startsWith("/") || topic.endsWith("/") || topic.contains("//")) it.remove();
		}
	}

}
