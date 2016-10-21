package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Instances of monotone classes can not be deleted or modified, once they have
 * been (de-)serialized.
 * 
 * @author Timm Felden
 */
public class MonotoneRestriction extends Restriction {

	@Override
	public String getName() {
		return "monotone";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
