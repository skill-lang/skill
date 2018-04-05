/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * The abstract restrictions.
 *
 * @author Timm Felden
 */
final public class AbstractRestriction extends Restriction {

	@Override
	public String getName() {
        return "abstract";
	}

	@Override
	public String toString() {
		return "@abstract";
	}

}
