/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * Makes pointers use i64 instead of v64, which is useful for lazy
 * deserialization of individual objects.
 * 
 * @author Timm Felden
 */
final public class ConstantLengthPointerRestriction extends Restriction {

	@Override
	public String getName() {
		return "constantLengthPointer";
	}

	@Override
	public String toString() {
        return "@ConstantLengthPointer";
	}

}
