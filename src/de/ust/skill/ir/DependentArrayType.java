package de.ust.skill.ir;

/**
 * The length of this array depends on another variable of the same declaration.
 * Due to canonical field order, this variable is stored somewhere before the
 * array.
 * 
 * @author Timm Felden
 */
public class DependentArrayType extends ArrayType {
	private final Type baseType;
	private final String fieldName;

	public DependentArrayType(Type baseType, String fieldName) {
		this.baseType = baseType;
		this.fieldName = fieldName;
	}

	public Type getBaseType() {
		return baseType;
	}
	
	public String getFieldName() {
		return fieldName;
	}
}
