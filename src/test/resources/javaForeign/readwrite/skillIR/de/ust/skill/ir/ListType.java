package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * @author Timm Felden
 */
public class ListType extends ContainerType implements SingleBaseTypeContainer {
	public Type baseType;

	public static Type make(TypeContext tc, Type baseType) {
		return tc.unifyType(new ListType(baseType));
	}

	public ListType(Type baseType) {
		this.baseType = baseType;
	}

	@Override
	public Type getBaseType() {
		return baseType;
	}

	@Override
	public String getSkillName() {
		return "list<" + baseType.getSkillName() + ">";
	}

    @Override
    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {
        Type sub = substitution.substitute(tc, baseType);
        if (sub instanceof ContainerType)
            throw new ParseException("Can not substitute a containertype into a map: " + sub);
        return make(tc, sub);
    }
}
