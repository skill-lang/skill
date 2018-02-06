/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir.restriction;

/**
 * @author Dennis Przytarski
 */
final public class FloatDefaultRestriction extends DefaultRestriction {

	private final double value;

	public FloatDefaultRestriction(double value) {
		this.value = value;
	}

	public double getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "@default(" + value + ")";
	}

}
