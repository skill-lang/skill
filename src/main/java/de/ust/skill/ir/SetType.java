package de.ust.skill.ir;

/**
 * @author Timm Felden
 */
public class SetType extends ContainerType {
	private final Type baseType;

	public static Type make(Type baseType) {
		return unifyType(new SetType(baseType));
	}

	private SetType(Type baseType) {
		this.baseType = baseType;
	}

	public Type getBaseType() {
		return baseType;
	}

	@Override
	public String getSkillName() {
		return "set<" + baseType.getSkillName() + ">";
	}
}
