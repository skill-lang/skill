/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir.restriction;

import de.ust.skill.ir.Restriction;

/**
 * @author Timm Felden
 */
final public class CodingRestriction extends Restriction {

	private final String value;

	public CodingRestriction(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "@coding(\"" + value + "\")";
	}

    @Override
    public String getName() {
        return "coding";
    }

}
