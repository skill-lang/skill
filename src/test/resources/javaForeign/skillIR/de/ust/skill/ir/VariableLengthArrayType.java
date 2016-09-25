package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * Encodes the length in a prefixed v64 value.
 * 
 * @author Timm Felden
 */
public class VariableLengthArrayType extends ContainerType implements SingleBaseTypeContainer {
    public final Type baseType;

    public static Type make(TypeContext tc, Type baseType) {
        return tc.unifyType(new VariableLengthArrayType(baseType));
    }

    public VariableLengthArrayType(Type baseType) {
        this.baseType = baseType;
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public String getSkillName() {
        return baseType.getSkillName() + "[]";
    }

    @Override
    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {
        Type sub = substitution.substitute(tc, baseType);
        if (sub instanceof ContainerType)
            throw new ParseException("Can not substitute a containertype into a map: " + sub);
        return make(tc, sub);
    }
}
