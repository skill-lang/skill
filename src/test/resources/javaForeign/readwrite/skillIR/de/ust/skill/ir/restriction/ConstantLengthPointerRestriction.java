package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Makes pointers use i64 instead of v64, which is useful for lazy
 * deserialization of individual objects.
 * 
 * @author Timm Felden
 */
public class ConstantLengthPointerRestriction extends Restriction {

	@Override
	public String getName() {
		return "constantLengthPointer";
	}

	@Override
	public String toString() {
        return "@ConstantLengthPointer";
	}

}
