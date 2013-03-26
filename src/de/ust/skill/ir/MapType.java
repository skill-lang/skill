package de.ust.skill.ir;

import java.util.List;

/**
 * @author Timm Felden
 */
public class MapType extends CompoundType {
	private final List<Type> baseType;

	public MapType(List<Type> baseType) {
		this.baseType = baseType;
	}

	public List<Type> getBaseType() {
		return baseType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("map<");
		boolean comma = false;
		for (Type t : baseType) {
			if (comma)
				sb.append(", ");
			sb.append(t.getTypeName());
			comma = true;
		}
		sb.append(">");
		return sb.toString();
	}

	@Override
	public String getTypeName() {
		return toString();
	}
}
