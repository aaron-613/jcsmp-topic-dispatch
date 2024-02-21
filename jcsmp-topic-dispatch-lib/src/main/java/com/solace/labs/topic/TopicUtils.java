package com.solace.labs.topic;

import java.util.regex.Pattern;

public final class TopicUtils {
	
	/**
	 * <p>This method validates whether a topic subscription used by the <code>MessageListenerWithTopicDispatch</code>
	 * class is well-formed.  This does not mean it isn't a valid subscription that would be accepted
	 * by the Solace broker.  But it does ensure it behave as expected.  That is:</p>
	 * <ul>
	 *   <li>Wildcards * and &gt; are not treated as literals
	 *   <ul>
	 *     <li>wildcard * must be the last char, or be immediately followed by /</li>
	 *     <li>wildcard &gt; must be the only char, or the last char immediately preceded by /</li>
	 *   </ul></li>
	 *   <li>No empty topic levels (subscription cannot start or end with /, or contain //</li>
	 * </ul>
	 * <p>The use of * and &gt; in a subscription as non-wildcard characters is valid (e.g. a*b must match exactly
	 * a published topic of a*b), however this use is discouraged, and will not be accepted by this validator.</p>
	 * @param sub String: e.g. "<code>a/b/c</code>", "<code>a/b*</code>", "<code>a/b*​/&gt;</code>"  
	 * @return
	 */
	public static boolean validateSubLinear(final String sub) {
		// easy checks first...
		if (sub == null || sub.isEmpty()) return false;
		if (sub.startsWith("/") || sub.endsWith("/")) return false;
		if ("*".equals(sub) || ">".equals(sub)) return true;
		if (sub.startsWith(">")) return false;
		// now walk the sub and check each char
		for (int subIndex=0; subIndex < sub.length(); subIndex++) {
			switch (sub.charAt(subIndex)) {
			case '*':  // valid if last char, or next char is a /
				if (subIndex != sub.length()-1 && sub.charAt(subIndex+1) != '/') return false;  // look ahead
				break;
			case '>':  // valid if preceding char is / and is the last character
				if (subIndex != sub.length()-1) return false;
				if (sub.charAt(subIndex-1) != '/') return false;  // look behind
				break;
			case '/':  // valid if previous char isn't / (empty level)
				if (subIndex == 0 ||						// if on first char
						sub.charAt(subIndex-1) == '/' ||	// or previous char isn't a slash /
						subIndex == sub.length()-1) {		// or we're the last char
					return false;  // if on first char, or look behind
				}
				break;
			default:
				if (sub.charAt(subIndex) == 0) return false;  // can't be null terminated
				// any other character is fine!
			}
		}
		return true;
	}
	
	/**
	 * <p><b>NOTE:</b> This regex method is slower than {@link #validateSubLinear(String)}</p>
	 * <p>This method validates whether a topic subscription used by the <code>MessageListenerWithTopicDispatch</code>
	 * class is well-formed.  This does not mean it isn't a valid subscription that would be accepted
	 * by the Solace broker.  But it does ensure it behave as expected.  That is:</p>
	 * <ul>
	 *   <li>Wildcards * and &gt; are not treated as literals<ul>
	 *     <li>wildcard * must be the last char, or be immediately followed by /</li>
	 *     <li>wildcard &gt; must be the only char, or the last char immediately preceded by /</li>
	 *   </ul></li>
	 *   <li>No empty topic levels (subscription cannot start or end with /, or contain //</li>
	 * </ul>
	 * <p>The use of * and &gt; in a subscription as non-wildcard characters is valid (e.g. a*b must match exactly
	 * a published topic of a*b), however this use is discouraged, and will not be accepted by this validator.</p>
	 * @param sub String: e.g. "a/b/c", "a/b*", "a/b*​/&gt;"
	 * @return
	 */	
	public static boolean validateSubRegex(final String sub) {
		// easy checks first...
		if (sub == null || sub.isEmpty()) return false;
		if (sub.startsWith("/") || sub.endsWith("/")) return false;
		if ("*".equals(sub) || ">".equals(sub)) return true;
		if (sub.startsWith(">")) return false;
		// slower checks now...
		if (sub.contains("//")) return false;  // empty topic level
		if (sub.matches(".*\\*[^/].*")) return false;  // a * wildcard with anything other than / following
		if (sub.contains(">")) {
			if (!sub.matches("[^>]*/>$")) return false;  // a > wildcard with anything other than / preceding, and has to be at end
		}
		return true;
	}

	/* Called by the Sub() constructor since it already validates */
	static String buildSubRegexNoChecks(final String sub) {
		assert validateSubLinear(sub);
		boolean print = false;  // debug helper
		if (print) System.out.print(sub);
		
		// let's do the easy, singleton wildcards one first
		if (sub.equals(">")) {
			String subRegex = "^\\Q.+\\E$";
			if (print) System.out.println(" -> " + subRegex);
			return subRegex;
		} else if (sub.equals("#") || sub.equals("\03")) {
			String subRegex = "^\\Q.*\\E$";
			if (print) System.out.println(" -> " + subRegex);
			return subRegex;
		}

		String subRegex = "^\\Q" + sub + "\\E$";  // include start and end, and escape the whole string
		if (print) System.out.print(" 1-> " + subRegex);
        
		// replace all * with a non-greedy .*, but need to 'unquote'
		subRegex = subRegex.replaceAll("\\*", "\\\\E[^/]*\\\\Q");  // four \ because we need to escape the \, and \\ is a \
		if (print) System.out.print(" 2-> " + subRegex);

		subRegex = subRegex.replace("/>", "/\\E.*\\Q");  // there better only be one at the end!
		if (print) System.out.print(" 3-> " + subRegex);

		subRegex = subRegex.replaceAll("/#\\\\E\\$$", "\\\\E(?:/.*)?\\\\Q");  // there better only be one!
//		subRegex = subRegex.replaceAll("/#\\\\E\\$$", "__blah__");  // there better only be one!
		if (print) System.out.print(" 4-> " + subRegex);

		// get rid of unnecessary open/close quote pairs
		subRegex = subRegex.replaceAll("\\\\Q\\\\E", "");
		if (print) System.out.printf(" -> %s", subRegex);
		
		// finally, need some special handing for singleton > or # wildcard subs
//		if (subRegex.equals("^\\Q>\\E$")) {
//			subRegex = "^\\Q.+\\E$";
//			if (print) System.out.printf(" -> %s", subRegex);
//		} else if (subRegex.equals("^\\Q#\\E$")) {
//			subRegex = "^\\Q.*\\E$";
//			if (print) System.out.printf(" -> %s", subRegex);
//		}
		if (print) System.out.println();
		return subRegex;
	}
	
	/**
	 * Build a regular expression to do SMF-style wildcard topic subscription mapping.
	 * Also includes use of the MQTT-style wildcard '#', which must occur 
	 */
	public static String buildSubRegex(final String sub) {
		if (!validateSubLinear(sub)) {
			throw new IllegalArgumentException("Sub '" + sub + "' is not well-fomed");
		}
		return buildSubRegexNoChecks(sub);
	}

	/**
	 * Convenience method for <code>Pattern.compile(buildSubRegex(sub))</code><p> 
	 * This assumes you have a valid subscription, i.e. not multiple &gt; for example
	 */
	public static Pattern buildSubRegexPattern(final String sub) {
		return Pattern.compile(buildSubRegex(sub));
	}

	public static boolean topicMatchesRegex(String topic, final Pattern sub) {
		return sub.matcher(topic).matches();
	}

	/** assumes that the subscription is well-formed i.e. wildcards &gt; and * not used as literals, no empty levels */
	public static boolean topicMatches(String topic, String sub) {
//		if (!validateSubLinear(sub)) return false;
		int topicIndex = 0;
		int subIndex = 0;
		for (topicIndex = 0; topicIndex < topic.length(); topicIndex++) {  // loop through each char of topic
			if (subIndex >= sub.length()) return false;  // have run out of sub chars, but topic still going, so die
			switch (sub.charAt(subIndex)) {  // what sub char am I on right now?
			case '*':  // single-level wildcard
				if (topic.charAt(topicIndex) == '/') {  // end of topic level
					subIndex++;  // advance sub index
					if (subIndex == sub.length()) return false;  // have run out of sub chars, and still another topic level to go
					// check next char in the sub, it better be a /
					if (sub.charAt(subIndex) == '/') {  // that's good, matching /
						subIndex++;  // advance sub index again, hopefully there's still more characters, will check on next loop
					} else {  // that means sub is malformed, have something like a*b
						return false;
					}
				}  // else it's not a slash, and it will match my * wildcard; ok to move to next topic char
				break;
			case '>':
				// this will match anything, actually we're done now if we made it this far
				return true;
			case '#':
				// only if we're at the end of the sub and the char before is / or nothing
				// otherwise treat it like a normal char below, let it fall through to default
				if (subIndex == sub.length() - 1
						&& ((sub.length() == 1 || (/* sub.length() >= 2 && */ sub.charAt(subIndex-1) == '/')))) {
					return true;
				}
				// else, treat as normal char, fall to default block
			default:
				// sub not on a wildcard, so we have to match exactly
				if (topic.charAt(topicIndex) != sub.charAt(subIndex)) return false;
				subIndex++;  // otherwise it does match, so advance the sub index
				break;
			}
		}
		// have run out of topic chars, but is sub at the end?
		// if the subIndex is at the end, but the last sub char is either * or > (and we've matched up to now)
		// OR subIndex is now just past length of sub (so both topic and sub are over) then DONE
		if ((subIndex == sub.length()-1 && (sub.charAt(subIndex) == '*' || sub.charAt(subIndex) == '>'))  // trailing slash on topic actually allowed
				|| subIndex == sub.length()) return true;
		// now special case for trailing /# 0-or-more
		if (subIndex >= sub.length()-2 && (sub.endsWith("/#")))  // we've matched up to /#, everything else is ok
			 return true;
		return false;
	}
	
	
	/**
	 * All of the regex pattern matching in this class/package assume that your subscriptions are well-formed.
	 * That is: it should be noted that the characters '*' and '&gt;' can be treated as literals by the broker
	 * if they appear in weird places.  So we make some assumptions such as:
	 * <ul>
	 * <li>If a multi-level wildcard char exists '&gt;': it must be the last char and follow a topic level separator '/'</li>
	 * <li>If a single-level wildcard char exists '*': it must either be the last char of the string, or be immediately followed by a topic level separator '/'</li>
	 * </ul>
	 * This method was gonna try to match topic to subs without these "well-formed" considerations, treating 
	 * wildcard chars as literals.
	 * @param topic
	 * @param sub
	 * @return
	 */
	static boolean topicMatchesDontUseYet(String topic, String sub) {
		int topicIndex = 0;
		int subIndex = 0;
		boolean insideValidWildcard = false;
		for (topicIndex = 0; topicIndex < topic.length(); topicIndex++) {  // loop through each char of topic
			if (subIndex >= sub.length()) return false;  // have run out of sub chars, but topic still going, so die
			switch (sub.charAt(subIndex)) {  // what sub char am I on right now?
			case '*':  // single-level wildcard (usually)
				if (!insideValidWildcard) {  // possibly not valid, need to validate first
					if (subIndex == sub.length()-1 || sub.charAt(subIndex+1) == '/') {  // either at end of sub, or next char is /
						insideValidWildcard = true;
					}
				}
				if (insideValidWildcard) {  // wildcard matching rules
					if (topic.charAt(topicIndex) == '/') {  // end of topic level
						subIndex++;  // advance sub index (to slash)
						// we already did a check above if there are more chars and if it's a /
						subIndex++; // advance again to next level of topic (might have run out of sub chars though, will check on next loop)
						insideValidWildcard = false;  // reset flag
					}
				    // else it's not a slash, and it will match my * wildcard; ok to move to next topic char
				} else {  // else not a valid wildcard, so treat as literal
					if (topic.charAt(topicIndex) != '*') return false;
					subIndex++;  // advance the sub index
				}
				// old code now...
/*				if (subIndex < sub.length()-1 && sub.charAt(subIndex+1) != '/') {  // not at end of sub, and next sub char isn't /
					// means that this isn't a wildcard, treat as literal
					if (topic.charAt(topicIndex) != '*') return false;
					subIndex++;  // advance the sub index
					break;
				}
				// else it's a valid wildcard
				if (topic.charAt(topicIndex) == '/') {  // end of topic level
					subIndex++;  // advance sub index (to slash)
					// we already did a check above if there are more chars and if it's a /
					subIndex++; // advance again to next level of topic (might have run out of sub chars though, will check on next loop)
//					if (subIndex == sub.length()) return false;  // have run out of sub chars, and still another topic level to go
//					// check next char in the sub, it better be a /
//					if (sub.charAt(subIndex) == '/') {  // that's good, matching /
//						subIndex++;  // advance sub index again, hopefully there's still more characters, will check on next loop
//					} else {  // that means sub is malformed, have something like a*b
//						return false;
//					}
				}  // else it's not a slash, and it will match my * wildcard; ok to move to next topic char
*/				break;
			case '>':
				if (!insideValidWildcard) {  // possibly not valid, need to validate first
					if (sub.equals(">") || subIndex == sub.length()-1 && sub.charAt(subIndex-1) != '/') {  // either only char, or / preceding
						insideValidWildcard = true;
					}
				}
				if (insideValidWildcard) {
					return true;  // match anything else
				} else {  // treat this as a literal
					
				}
				
				
				// if this isn't the last sub char, or the sub char before isn't /
				if (subIndex < sub.length()-1 || subIndex > 0 && sub.charAt(subIndex-1) != '/') {
					// means that this isn't a wildcard, treat as literal
					if (topic.charAt(topicIndex) != '>') return false;
					subIndex++;  // advance the sub index
					break;
				}
				// this will match anything, actually we're done now if we made it this far
				return true;
			default:
				// sub not on a wildcard, so we have to match exactly
				if (topic.charAt(topicIndex) != sub.charAt(subIndex)) return false;
				subIndex++;  // otherwise it does match, so advance the sub index
				break;
			}
		}
		// have run out of topic chars, but is sub at the end?
		if (subIndex == sub.length()) {  // we're passed the end of the sub, so all matched
			return true;
		} else if (subIndex == sub.length()-1) {  // still pointing to the last sub char, could be wildcard?
			if (sub.charAt(subIndex) == '>') {  // better make sure it's proper
//				return (sub.char)
			}

		}
		return false;
	}

	
	
	private TopicUtils() {
		throw new AssertionError("Don't instantiate util class");
	}
}
