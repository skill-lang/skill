package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Ensures that a field does not contain a NULL.
 * 
 * @author Timm Felden
 */
final public class NonNullRestriction extends Restriction {

	@Override
	public String getName() {
		return "nullable";
	}

	@Override
	public String toString() {
		return "@nullable";
	}

}
