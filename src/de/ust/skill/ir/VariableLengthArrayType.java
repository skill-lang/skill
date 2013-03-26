package de.ust.skill.ir;

/**
 * Encodes the length in a prefixed v64 value.
 * 
 * @author Timm Felden
 */
public class VariableLengthArrayType extends ArrayType {
	private final Type baseType;

	public VariableLengthArrayType(Type baseType) {
		this.baseType = baseType;
	}

	public Type getBaseType() {
		return baseType;
	}
}
