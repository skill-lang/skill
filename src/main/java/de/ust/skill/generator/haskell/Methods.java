package de.ust.skill.generator.haskell;

class Methods {
	static String substring(String string, String start, String ending) {
		string = substring(string, start);

		if (string == null) { return null; }

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != ending.charAt(0)) {
				continue;
			} else if (string.substring(i).startsWith(ending)) { return string.substring(0, i); }
		}
		return null;
	}	
	
	static String substring(String string, String start) {
		if (start.isEmpty()) { return string; }

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != start.charAt(0)) {
				continue;
			} else if (string.substring(i, i + start.length()).equals(start)) { return string.substring(i + start.length()); }
		}
		return null;
	}

	static String substring(String string, int start, char end) {
		for (int i = start; i < string.length(); i++) {
			if (string.charAt(i) == end) { return string.substring(start, i); }
		}
		return null;
	}
}