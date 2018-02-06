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
public class SetType extends ContainerType implements SingleBaseTypeContainer {
	private final Type baseType;

	public static Type make(TypeContext tc, Type baseType) {
		return tc.unifyType(new SetType(baseType));
	}

	private SetType(Type baseType) {
		this.baseType = baseType;
	}

	@Override
	public Type getBaseType() {
		return baseType;
	}

	@Override
	public String getSkillName() {
		return "set<" + baseType.getSkillName() + ">";
	}

    @Override
    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {
        Type sub = substitution.substitute(tc, baseType);
        if (sub instanceof ContainerType)
            throw new ParseException("Can not substitute a containertype into a map: " + sub);
        return make(tc, sub);
    }
}
