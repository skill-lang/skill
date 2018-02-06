/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
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
