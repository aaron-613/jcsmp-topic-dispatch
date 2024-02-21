/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.solace.labs.topic;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.solace.labs.topic.TopicUtils;






public class SubValidationComparisonTests {
	    
	static Set<String> possibleSubs = new HashSet<>();
	static Set<String> validSubs = new HashSet<>();
	static Set<String> regexSubs = new HashSet<>();

	
	
	@BeforeClass
	public static void generatePossibleSubs() {
		int howMany = 100_000;
		System.out.printf("building %,d random possible subs... ", howMany);
		for (int i=0; i<howMany; i++) {
			possibleSubs.add(TestDataGeneratorUtil.buildRandomSub(20));
		}
		System.out.println("Done.");
	}

	
	
    @Test
    public void subValidation() {
    	long start = System.currentTimeMillis();
    	for (String sub : possibleSubs) {
    		if (TopicUtils.validateSubLinear(sub)) validSubs.add(sub);
    	}
    	System.out.printf("NON-regex validation of %,d subs took %,d ms.%n", possibleSubs.size(), System.currentTimeMillis()-start);
    	assertTrue("this is true", true);
    }

    @Test
    public void regexSubValidation() {
    	long start = System.currentTimeMillis();
    	for (String sub : possibleSubs) {
    		if (TopicUtils.validateSubRegex(sub)) {
    			regexSubs.add(sub);
    		}
    	}
    	System.out.printf("Regex validation of %,d subs took %,d ms.%n", possibleSubs.size(), System.currentTimeMillis()-start);
    	
    	start = System.currentTimeMillis();
    	for (String sub : regexSubs) {
    		TopicUtils.buildSubRegexPattern(sub);
    	}
    	System.out.printf("Regex pattern construction of %,d subs took %,d ms.%n", regexSubs.size(), System.currentTimeMillis()-start);
    	
    	assertTrue("this is true", true);
    }
    
    @AfterClass
    public static void checkSets() {
    	if (validSubs.equals(regexSubs)) {
    		System.out.printf("Sets are same, both have %,d elements.%n", regexSubs.size());
    	} else {
    		System.out.printf("Sets are different, %,d vs. %,d reg vs. regex%n", validSubs.size(), regexSubs.size());
    		Set<String> diffs = new HashSet<>(regexSubs);
    		diffs.removeAll(validSubs);
    		System.out.println("Regular: " + validSubs);
    		System.out.println("Regex:   " + regexSubs);
    		System.out.println("Diff:    " + diffs);
    	}
    }
}