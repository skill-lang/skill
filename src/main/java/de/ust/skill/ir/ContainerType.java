package de.ust.skill.ir;

/**
 * @author Timm Felden
 */
public abstract class ContainerType extends Type {
	@Override
    final public Name getName() {
        throw new NoSuchMethodError("container types shall not be used that way");
	}
}
