package de.ust.skill.ir;

/**
 * The length of the array is a constant, which is known at compile time.
 * 
 * @author Timm Felden
 */
public class ConstantLengthArrayType extends ArrayType {
	private final Type baseType;
	private final long length;
	
	public ConstantLengthArrayType(Type baseType, long length){
		this.baseType = baseType;
		this.length = length;
	}
	
	public Type getBaseType() {
		return baseType;
	}
	
	public long getLength() {
		return length;
	}
}
