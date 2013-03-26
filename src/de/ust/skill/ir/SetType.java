package de.ust.skill.ir;

/**
 * @author Timm Felden
 */
public class SetType extends CompoundType {
	private final Type baseType;
	
	public SetType(Type baseType){
		this.baseType=baseType;
	}
	
	public Type getBaseType() {
		return baseType;
	}
}
