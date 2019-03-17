package de.ust.skill.ir.restriction;

/**
 * @author Dennis Przytarski
 */
final public class StringDefaultRestriction extends DefaultRestriction {

	private final String value;

	public StringDefaultRestriction(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "@default(\"" + value + "\")";
	}

}
