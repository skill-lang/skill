package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of a field declaration.
 * 
 * @author Timm Felden
 */
public class Field {
    protected final boolean auto;
    protected final boolean isConstant;
    protected final long constantValue;

    protected final Name name;
    protected final Type type;

    protected Declaration declaredIn;

    /**
     * The restrictions applying to this field.
     */
    protected final List<Restriction> restrictions;
    /**
     * The restrictions applying to this field.
     */
    protected final Set<Hint> hints;
    /**
     * The comment from Specification.
     */
    protected final Comment comment;

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
        assert(null != type);
        assert(null != name);
        if (!(type instanceof GroundType))
            throw new ParseException("Can not create a constant of non-integer type " + type);
        if (!((GroundType) type).isInteger())
            throw new ParseException("Can not create a constant of non-integer type " + type);

        auto = false;
        isConstant = true;
        constantValue = value;
        this.name = name;
        this.type = type;
        this.comment = comment;
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
        assert(null != type);
        assert(null != name);

        isConstant = false;
        constantValue = 0;
        auto = isAuto;
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.restrictions = restrictions;
        this.hints = Collections.unmodifiableSet(new HashSet<Hint>(hints));
        Hint.checkField(this, this.hints);
    }

    protected Field(boolean auto, boolean isConstant, long constantValue, Name name, Type type, Declaration declaredIn,
            ArrayList<Restriction> restrictions, HashSet<Hint> hints, Comment comment) {
        this.auto = auto;
        this.isConstant = isConstant;
        this.constantValue = constantValue;
        this.name = name;
        this.type = type;
        this.declaredIn = declaredIn;
        this.restrictions = restrictions;
        this.hints = hints;
        this.comment = comment;
    }

    public Field cloneWith(Type newType, Collection<Restriction> nrs, Collection<Hint> nhs) {
        ArrayList<Restriction> rs = new ArrayList<>(nrs);
        rs.addAll(restrictions);
        HashSet<Hint> hs = new HashSet<>(nhs);
        hs.addAll(hints);
        return new Field(auto, isConstant, constantValue, name, newType, declaredIn, rs, hs, comment);
    }

    public Name getName() {
        return name;
    }

    public String getSkillName() {
        return name.lower();
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
        sb.append(type.getSkillName()).append(" ").append(name);
        if (isConstant)
            sb.append(" = ").append(constantValue);

        return sb.toString();
    }

    public Comment getComment() {
        return comment;
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

    /**
     * @return the enclosing declaration.
     */
    public Declaration getDeclaredIn() {
        return declaredIn;
    }

    /**
     * Invoked during construction of the enclosing declaration.
     */
    void setDeclaredIn(Declaration declaredIn) {
        this.declaredIn = declaredIn;
    }
}
