/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * @author Timm Felden
 */
public abstract class ContainerType extends Type {
	@Override
    final public Name getName() {
        throw new NoSuchMethodError("container types shall not be used that way");
	}

    public abstract Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException;
}
