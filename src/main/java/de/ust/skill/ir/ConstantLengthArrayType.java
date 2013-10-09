package de.ust.skill.ir;

/**
 * The length of the array is a constant, which is known at compile time.
 * 
 * @author Timm Felden
 */
public class ConstantLengthArrayType extends ContainerType {
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
}
