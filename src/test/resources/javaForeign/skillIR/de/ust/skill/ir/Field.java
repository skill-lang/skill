package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of a field declaration.
 * 
 * @author Timm Felden
 */
final public class Field extends FieldLike {
    public final boolean auto;
    public final boolean isConstant;
    public final long constantValue;

    public final Type type;

    /**
     * The restrictions applying to this field.
     */
    public final List<Restriction> restrictions;
    /**
     * The restrictions applying to this field.
     */
    public final Set<Hint> hints;

    /**
     * Constructor for constant fields.
     * 
     * @param type
     * @param name
     * @param value
     * @throws ParseException
     *             if the argument type is not an integer or if illegal hints are used
     */
    public Field(Type type, Name name, long value, Comment comment, List<Restriction> restrictions, List<Hint> hints)
            throws ParseException {
        super(name, comment);
        
        if (!(type instanceof GroundType))
            throw new ParseException("Can not create a constant of non-integer type " + type);
        if (!((GroundType) type).isInteger())
            throw new ParseException("Can not create a constant of non-integer type " + type);

        auto = false;
        isConstant = true;
        constantValue = value;
        this.type = type;
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
    public Field(Type type, Name name, boolean isAuto, Comment comment, List<Restriction> restrictions,
            Collection<Hint> hints) throws ParseException {
        super(name, comment);
        

        isConstant = false;
        constantValue = 0;
        auto = isAuto;
        this.type = type;
        this.restrictions = restrictions;
        this.hints = Collections.unmodifiableSet(new HashSet<Hint>(hints));
        Hint.checkField(this, this.hints);
    }

    public Field(boolean auto, boolean isConstant, long constantValue, Name name, Type type, Declaration declaredIn,
            ArrayList<Restriction> restrictions, HashSet<Hint> hints, Comment comment) {
        super(name, comment);
        this.auto = auto;
        this.isConstant = isConstant;
        this.constantValue = constantValue;
        this.type = type;
        this.declaredIn = declaredIn;
        this.restrictions = restrictions;
        this.hints = hints;
    }

    public Field cloneWith(Type newType, Collection<Restriction> nrs, Collection<Hint> nhs) {
        ArrayList<Restriction> rs = new ArrayList<>(nrs);
        rs.addAll(restrictions);
        HashSet<Hint> hs = new HashSet<>(nhs);
        hs.addAll(hints);
        return new Field(auto, isConstant, constantValue, name, newType, declaredIn, rs, hs, getComment());
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
        
        return constantValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isConstant)
            sb.append("const ");
        if (auto)
            sb.append("auto ");
        sb.append(type.getSkillName()).append(" ").append(name);
        if (isConstant)
            sb.append(" = ").append(constantValue);

        return sb.toString();
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

    public Collection<Hint> getHints() {
		return hints;
	}

	/**
	 * Search hints for a pragma of argument name. Return null, if not present.
	 *
	 * @param id
	 *            name of the pragma
	 * @return list of arguments, if present, null else
	 */
	public List<Name> getPragma(String id) {
		Optional<Hint> hint = hints.stream().filter(
				h -> Hint.Type.pragma == h.type() && h.arguments().get(0).getSkillName().equals(id.toLowerCase()))
				.findFirst();
		if (hint.isPresent()) {
			List<Name> args = hint.get().arguments();
			return args.subList(1, args.size());
		}
		return null;
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
