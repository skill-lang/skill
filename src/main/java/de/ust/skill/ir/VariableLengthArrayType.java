package de.ust.skill.ir;

/**
 * Encodes the length in a prefixed v64 value.
 * 
 * @author Timm Felden
 */
public class VariableLengthArrayType extends ContainerType {
	private final Type baseType;

	public static Type make(TypeContext tc, Type baseType) {
		return tc.unifyType(new VariableLengthArrayType(baseType));
	}

	private VariableLengthArrayType(Type baseType) {
		this.baseType = baseType;
	}

	public Type getBaseType() {
		return baseType;
	}

	@Override
	public String getSkillName() {
		return baseType.getSkillName() + "[]";
	}
}
