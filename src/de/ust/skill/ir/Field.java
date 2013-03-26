package de.ust.skill.ir;

public abstract class Field implements Comparable<Field> {
	protected final String name, canonicalName;
	protected final Type type;

	protected Field(Type type, String name) {
		if (null == type)
			throw new IllegalArgumentException("type is null");
		if (null == name)
			throw new IllegalArgumentException("name is null");

		this.name = name;
		this.canonicalName = name.toLowerCase();
		this.type = type;
	}

	/**
	 * implements canonical field order
	 */
	@Override
	public int compareTo(Field f) {
		int rval = type.compareTo(f.type);
		if (0 == rval)
			return canonicalName.compareTo(f.canonicalName);
		return rval;
	}
}
