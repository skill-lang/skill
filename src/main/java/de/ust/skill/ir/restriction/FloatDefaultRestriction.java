package de.ust.skill.ir.restriction;

/**
 * @author Dennis Przytarski
 */
final public class FloatDefaultRestriction extends DefaultRestriction {

	private final double value;

	public FloatDefaultRestriction(double value) {
		this.value = value;
	}

	public double getValue() {
		return this.value;
	}

}
