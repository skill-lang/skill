package de.ust.skill.ir;

import java.util.List;

/**
 * @author Timm Felden
 */
public class MapType extends CompoundType {
	private final List<Type> baseType;
	
	public MapType(List<Type> baseType){
		this.baseType=baseType;
	}
	
	public List<Type> getBaseType() {
		return baseType;
	}
}
