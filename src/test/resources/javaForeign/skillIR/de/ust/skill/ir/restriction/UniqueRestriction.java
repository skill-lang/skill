package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Instances of unique types are pair-wise distinct.
 * 
 * @author Timm Felden
 */
public class UniqueRestriction extends Restriction {

	@Override
	public String getName() {
		return "unique";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
