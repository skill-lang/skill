package de.ust.skill.ir;

final public class Field {
	private final boolean auto;
	private final boolean isConstant;
	private final long constantValue;

	protected final String name, skillName;
	protected final Type type;

	/**
	 * Constructor for constant fields.
	 * 
	 * @param type
	 * @param name
	 * @param value
	 */
	public Field(Type type, String name, long value) {
		assert (null != type);
		assert (null != name);

		auto = false;
		isConstant = true;
		constantValue = value;
		this.name = name;
		skillName = name.toLowerCase();
		this.type = type;
	}

	/**
	 * Constructor for auto and data fields.
	 * 
	 * @param type
	 * @param name
	 * @param isAuto
	 */
	public Field(Type type, String name, boolean isAuto) {
		assert (null != type);
		assert (null != name);

		isConstant = false;
		constantValue = 0;
		auto = isAuto;
		this.name = name;
		this.skillName = name.toLowerCase();
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getSkillName() {
		return skillName;
	}

	public Type getType() {
		return type;
	}

	public boolean isAuto() {
		return auto;
	}

	public boolean isConstant() {
		return isConstant;
	}

	public long constantValue() {
		assert isConstant : "only constants have a constant value";
		return constantValue;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (isConstant)
			sb.append("const ");
		if (auto)
			sb.append("auto ");
		sb.append(type.getSkillName()).append(" ").append(skillName);
		if (isConstant)
			sb.append(" = ").append(constantValue);

		return sb.toString();
	}
}
