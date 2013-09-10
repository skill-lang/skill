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

	public String getName() {
		return name;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public Type getType() {
		return type;
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

	@Override
	public String toString() {
		return type.getTypeName() + " " + canonicalName;

	}
}
