package de.ust.skill.ir.restriction;

/**
 * @author Dennis Przytarski
 */
public class FloatDefaultRestriction extends DefaultRestriction {

	public double value;

	public FloatDefaultRestriction(double value) {
		this.value = value;
	}

	public double getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "@default(" + value + ")";
	}

}
