/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
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
        return "nonnull";
	}

	@Override
	public String toString() {
        return "@nonnull";
	}

}
