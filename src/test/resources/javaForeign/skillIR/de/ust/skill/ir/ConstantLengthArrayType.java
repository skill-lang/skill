package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * The length of the array is a constant, which is known at compile time.
 * 
 * @author Timm Felden
 */
public class ConstantLengthArrayType extends ContainerType  implements SingleBaseTypeContainer{
	public Type baseType;
	public long length;

	public static Type make(TypeContext tc, Type baseType, long length) throws ParseException {
		if (length < 0)
			throw new ParseException("Constant array length can not be negative.");

		return tc.unifyType(new ConstantLengthArrayType(baseType, length));
	}

	public ConstantLengthArrayType(Type baseType, long length) {
		this.baseType = baseType;
		this.length = length;
	}

	@Override
	public Type getBaseType() {
		return baseType;
	}

	public long getLength() {
		return length;
	}

	@Override
	public String getSkillName() {
		return baseType.getSkillName() + "[" + length + "]";
	}

    @Override
    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {
        Type sub = substitution.substitute(tc, baseType);
        if (sub instanceof ContainerType)
            throw new ParseException("Can not substitute a containertype into a map: " + sub);
        return make(tc, sub, length);
    }
}
