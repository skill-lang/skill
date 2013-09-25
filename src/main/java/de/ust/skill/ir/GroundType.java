package de.ust.skill.ir;

/**
 * Implementation of all Ground Types, i.e. strings, integers....
 * 
 * @author Timm Felden
 */
public class GroundType extends Type {

	/**
	 * String representation of the type.
	 */
	private final String name;

	GroundType(TypeContext tc, String name) {
		this.name = name;
		tc.unifyType(this);
	}

	public boolean isInteger() {
		return name.charAt(0) == 'i' || name.charAt(0) == 'v';
	}

	public boolean isFloat() {
		return name.charAt(0) == 'f';
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSkillName() {
		return name;
	}

	@Override
	public String getCapitalName() {
		throw new NoSuchMethodError("capital names of ground types shall not be used");
	}
}
