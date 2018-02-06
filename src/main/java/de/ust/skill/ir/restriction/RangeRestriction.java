/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
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
abstract public class RangeRestriction extends Restriction {

	@Override
	public String getName() {
		return "range";
	}

	@Override
	public String toString() {
		throw new NoSuchMethodError("not yet implemented");
	}

}
