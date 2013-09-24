package de.ust.skill.ir;

import java.util.List;

/**
 * @author Timm Felden
 */
public class MapType extends ContainerType {
	private final List<Type> baseTypes;

	public static Type make(List<Type> baseTypes) {
		return unifyType(new MapType(baseTypes));
	}

	private MapType(List<Type> baseTypes) {
		this.baseTypes = baseTypes;
	}

	public List<Type> getBaseTypes() {
		return baseTypes;
	}

	@Override
	public String getSkillName() {
		StringBuilder sb = new StringBuilder("map<");
		boolean comma = false;
		for (Type t : baseTypes) {
			if (comma)
				sb.append(",");
			sb.append(t.getSkillName());
			comma = true;
		}
		sb.append(">");
		return sb.toString();
	}
}
