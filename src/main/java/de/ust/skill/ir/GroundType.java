package de.ust.skill.ir;

import java.util.HashMap;
import java.util.Set;

public class GroundType extends Type {
	private static final HashMap<String, GroundType> types = new HashMap<>();
	static {
		// two v64
		new GroundType("annotation", -1);

		new GroundType("bool", 1);

		new GroundType("i1", 1);
		new GroundType("i8", 1);
		new GroundType("i16", 2);
		new GroundType("i32", 4);
		new GroundType("i64", 8);

		new GroundType("v64", -1);

		new GroundType("f32", 4);
		new GroundType("f64", 8);

		new GroundType("string", -1);
	}

	/**
	 * String representation of the type.
	 */
	private final String name;
	/**
	 * Size in bytes or -1 if this can not be determined in general.
	 */
	private final int size;

	private GroundType(String name, int size) {
		this.name = name;
		this.size = size;
		types.put(name, this);
	}

	public static GroundType get(String name) {
		return types.get(name);
	}

	public static Set<String> allTypeNames() {
		return types.keySet();
	}

	public String getName() {
		return name;
	}

	public int getSize() {
		return size;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String getTypeName() {
		return name;
	}
}
