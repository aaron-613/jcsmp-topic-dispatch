/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.solace.labs.topic;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import com.solace.labs.topic.TopicUtils;

/** Compares the results of topic matching of both regular and regex subs */
public class TopicMatchingValidityTests {
	    
	static Set<String> topics = new HashSet<>();
	static Set<String> validSubs = new HashSet<>();
	
	@BeforeClass
	public static void populateData() {
		for (int i=0; i<10_000; i++) {
			String sub = TestDataGeneratorUtil.buildRandomSub(12);
			if (TopicUtils.validateSubLinear(sub)) validSubs.add(sub);
		}
		for (int i=0; i<10_000; i++) {
			topics.add(TestDataGeneratorUtil.buildRandomTopic(20));
		}
	}
	
    @Test
    public void topicMatchingValidation() {
    	for (String sub : validSubs) {
    		for (String topic : topics) {
    			Pattern p = TopicUtils.buildSubRegexPattern(sub);
    			if (TopicUtils.topicMatches(topic, sub) ^ TopicUtils.topicMatchesRegex(topic, p)) {
    				System.out.println("Sub: " + sub + ", Topic: " + topic);
    				System.out.println(TopicUtils.topicMatches(topic, sub));
    				System.out.println(TopicUtils.buildSubRegex(sub));
    				System.out.println();
    			}
    		}
    	}
    }

}
