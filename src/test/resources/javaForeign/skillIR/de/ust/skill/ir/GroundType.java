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
    public Name name;

    GroundType(TypeContext tc, String name) {
        this.name = new Name(name);
        tc.unifyType(this);
    }

    public boolean isInteger() {
        return name.lower().charAt(0) == 'i' || name.lower().charAt(0) == 'v';
    }

    public boolean isFloat() {
        return name.lower().charAt(0) == 'f';
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String getSkillName() {
        return name.getSkillName();
    }
}
