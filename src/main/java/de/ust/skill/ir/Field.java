package de.ust.skill.ir;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of a field declaration. TODO F
 * 
 * @author Timm Felden
 */
final public class Field {
	private final boolean auto;
	private final boolean isConstant;
	private final long constantValue;

	protected final String name, skillName;
	protected final Type type;

	/**
	 * The restrictions applying to this field.
	 */
	private final List<Restriction> restrictions;
	/**
	 * The restrictions applying to this field.
	 */
	private final Set<Hint> hints;
	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 */
	private final String skillCommentImage;

	/**
	 * Constructor for constant fields.
	 * 
	 * @param type
	 * @param name
	 * @param value
	 * @throws ParseException
	 *             if the argument type is not an integer or if illegal hints
	 *             are used
	 */
	public Field(Type type, String name, long value, String comment, List<Restriction> restrictions, List<Hint> hints)
			throws ParseException {
		assert (null != type);
		assert (null != name);
		if (!(type instanceof GroundType))
			throw new ParseException("Can not create a constant of non-integer type " + type);
		if (!((GroundType) type).isInteger())
			throw new ParseException("Can not create a constant of non-integer type " + type);

		auto = false;
		isConstant = true;
		constantValue = value;
		this.name = name;
		skillName = name.toLowerCase();
		this.type = type;
		skillCommentImage = null == comment ? "" : comment;
		this.restrictions = restrictions;
		this.hints = Collections.unmodifiableSet(new HashSet<Hint>(hints));
		Hint.checkField(this, this.hints);
	}

	/**
	 * Constructor for auto and data fields.
	 * 
	 * @param type
	 * @param name
	 * @param isAuto
	 * @throws ParseException
	 *             if illegal hints are used
	 */
	public Field(Type type, String name, boolean isAuto, String comment, List<Restriction> restrictions,
			List<Hint> hints) throws ParseException {
		assert (null != type);
		assert (null != name);

		isConstant = false;
		constantValue = 0;
		auto = isAuto;
		this.name = name;
		this.skillName = name.toLowerCase();
		this.type = type;
		skillCommentImage = null == comment ? "" : comment;
		this.restrictions = restrictions;
		this.hints = Collections.unmodifiableSet(new HashSet<Hint>(hints));
		Hint.checkField(this, this.hints);
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

	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 * 
	 * This may require further transformation depending on the target language.
	 * 
	 * @note can contain newline characters!!!
	 */
	public String getSkillComment() {
		return skillCommentImage;
	}

	public List<Restriction> getRestrictions() {
		return restrictions;
	}

	public boolean isUnique() {
		return hints.contains(Hint.unique);
	}

	public boolean isPure() {
		return hints.contains(Hint.pure);
	}

	public boolean isDistributed() {
        return hints.contains(Hint.distributed) || isOnDemand();
	}

    public boolean isOnDemand() {
        return hints.contains(Hint.ondemand);
	}

	public boolean isIgnored() {
		return hints.contains(Hint.ignore) || hasIgnoredType();
	}

	/**
	 * @return true, iff the field's type has an ignore hint
	 */
	public boolean hasIgnoredType() {
		if (type instanceof UserType)
			return ((UserType) type).isIgnored();
		return false;
	}
}
