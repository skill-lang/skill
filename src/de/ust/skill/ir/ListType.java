package de.ust.skill.ir;

/**
 * @author Timm Felden
 */
public class ListType extends CompoundType {
	private final Type baseType;
	
	public ListType(Type baseType){
		this.baseType=baseType;
	}
	
	public Type getBaseType() {
		return baseType;
	}
}
