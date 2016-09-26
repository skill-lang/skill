package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Base class for range restrictions.
 *
 * @see IntRangeRestriction
 * @see FloatRangeRestriction
 *
 * @author Timm Felden
 */
public class RangeRestriction extends Restriction {

	@Override
	public String getName() {
		return "range";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
