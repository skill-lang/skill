package de.ust.skill.ir;

import java.util.Collection;
import java.util.List;

/**
 * Definition of a typedef.
 * 
 * @author Timm Felden
 */
final public class Typedef extends Declaration {

    private Type target = null;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact
     *             illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in
     *       pre-order over the type hierarchy.
     */
    private Typedef(Name name, Comment comment, List<Restriction> restrictions, Collection<Hint> hints)
            throws ParseException {
        super(name, comment, restrictions, hints);
    }

    @Override
    Typedef copy(TypeContext tc) {
        try {
            return newDeclaration(tc, name, comment, restrictions, hints);
        } catch (ParseException e) {
            throw new Error("can not happen", e);
        }
    }

    /**
     * @param name
     * @return a new declaration which is registered at types.
     * @throws ParseException
     *             if the declaration is already present
     */
    public static Typedef newDeclaration(TypeContext tc, Name name, Comment comment, List<Restriction> restrictions,
            Collection<Hint> hints) throws ParseException {
        String skillName = name.getSkillName();
        if (tc.types.containsKey(skillName))
            throw new ParseException("Duplicate declaration of type " + name);

        Typedef rval = new Typedef(name, comment, restrictions, hints);
        tc.types.put(skillName, rval);
        return rval;
    }

    @Override
    public boolean isInitialized() {
        return null != target;
    }

    /**
     * Initializes the type declaration with data obtained from parsing the
     * declarations body.
     * 
     * @param SuperType
     * @param Fields
     * @throws ParseException
     *             thrown if the declaration is illegal, e.g. because it
     *             contains illegal hints
     */
    public void initialize(Type target) throws ParseException {
        assert !isInitialized() : "multiple initialization";
        this.target = target;
    }

    public Type getTarget() {
        return target;
    }

    /**
     * @return pretty parsable representation of this type
     */
    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("typedef ");
        sb.append(name.getSkillName()).append(" ").append(target.getSkillName());

        return sb.toString();
    }

    @Override
    public boolean isMonotone() {
        if (target instanceof UserType)
            return ((UserType) target).isMonotone();

        return false;
    }

    @Override
    public boolean isReadOnly() {
        if (target instanceof UserType)
            return ((UserType) target).isReadOnly();

        return false;
    }

    public Collection<Hint> getHints() {
        return hints;
    }
}
