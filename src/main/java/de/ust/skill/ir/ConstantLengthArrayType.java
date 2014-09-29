package de.ust.skill.ir;

import de.ust.skill.ir.internal.Substitution;

/**
 * The length of the array is a constant, which is known at compile time.
 * 
 * @author Timm Felden
 */
public class ConstantLengthArrayType extends ContainerType  implements SingleBaseTypeContainer{
	private final Type baseType;
	private final long length;

	public static Type make(TypeContext tc, Type baseType, long length) throws ParseException {
		if (length < 0)
			throw new ParseException("Constant array length can not be negative.");

		return tc.unifyType(new ConstantLengthArrayType(baseType, length));
	}

	private ConstantLengthArrayType(Type baseType, long length) {
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
        return make(tc, substitution.substitute(tc, baseType), length);
    }
}
