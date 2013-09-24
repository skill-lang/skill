package de.ust.skill.ir;

/**
 * @author Timm Felden
 */
public abstract class ContainerType extends Type {
	@Override
	final public String getName() {
		return getSkillName();
	}

	@Override
	final public String getCapitalName() {
		throw new NoSuchMethodError("container types shall not be used that way");
	}
}
