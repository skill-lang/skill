package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * @author Timm Felden
 */
public class ContainerType extends Type {
	@Override
    public Name getName() {
        throw new NoSuchMethodError("container types shall not be used that way");
	}

    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {return null;}
}
