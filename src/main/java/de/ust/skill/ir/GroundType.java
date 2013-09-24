package de.ust.skill.ir;

/**
 * Implementation of all Ground Types, i.e. strings, integers....
 * 
 * @author Timm Felden
 */
public class GroundType extends Type {
	static {
		// two v64
		new GroundType("annotation");

		new GroundType("bool");

		new GroundType("i8");
		new GroundType("i16");
		new GroundType("i32");
		new GroundType("i64");

		new GroundType("v64");

		new GroundType("f32");
		new GroundType("f64");

		new GroundType("string");
	}

	/**
	 * String representation of the type.
	 */
	private final String name;

	private GroundType(String name) {
		this.name = name;
		unifyType(this);
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
