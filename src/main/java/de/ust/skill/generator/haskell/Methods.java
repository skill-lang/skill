package de.ust.skill.generator.haskell;

public class Methods {
	public static String substring(String string, String start) {
		if (start.isEmpty()) { return string; }

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != start.charAt(0)) {
				continue;
			} else if (string.substring(i, i + start.length()).equals(start)) { return string.substring(i + start.length()); }
		}
		return null;
	}

	public static String substring(String string, int start, char end) {
		for (int i = start; i < string.length(); i++) {
			if (string.charAt(i) == end) { return string.substring(start, i); }
		}
		return null;
	}
}