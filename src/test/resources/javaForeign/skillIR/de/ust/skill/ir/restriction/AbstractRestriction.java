package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * The abstract restrictions.
 *
 * @author Timm Felden
 */
public class AbstractRestriction extends Restriction {

	@Override
	public String getName() {
        return "abstract";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
