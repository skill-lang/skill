package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * The default restrictions.
 *
 * @author Dennis Przytarski
 */
abstract public class DefaultRestriction extends Restriction {

	@Override
	public String getName() {
        return "default";
	}

	@Override
	abstract public String toString();

}
