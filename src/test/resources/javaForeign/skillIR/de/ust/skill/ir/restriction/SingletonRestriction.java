package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * The singleton restrictions.
 *
 * @author Timm Felden
 */
final public class SingletonRestriction extends Restriction {

	@Override
	public String getName() {
		return "singleton";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
