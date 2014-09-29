package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * Encodes the length in a prefixed v64 value.
 * 
 * @author Timm Felden
 */
public class VariableLengthArrayType extends ContainerType implements SingleBaseTypeContainer {
    private final Type baseType;

    public static Type make(TypeContext tc, Type baseType) {
        return tc.unifyType(new VariableLengthArrayType(baseType));
    }

    private VariableLengthArrayType(Type baseType) {
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
        return make(tc, substitution.substitute(tc, baseType));
    }
}
