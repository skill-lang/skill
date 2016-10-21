package de.ust.skill.ir.restriction;

/**
 * @author Dennis Przytarski
 */
public class IntDefaultRestriction extends DefaultRestriction {

	public long value;

	public IntDefaultRestriction(long value) {
		this.value = value;
	}

	public long getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "@default(" + value + ")";
	}
}
