package de.ust.skill.ir;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of an interface type.
 * 
 * @author Timm Felden
 */
final public class EnumType extends Declaration {

    // fields
    private List<Field> fields = null;
    private List<Name> instances;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact
     *             illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in
     *       pre-order over the type hierarchy.
     */
    private EnumType(Name name, String comment, List<Name> instances) throws ParseException {
        super(name, comment, Collections.<Restriction> emptyList(), Collections.<Hint> emptyList());
        this.instances = instances;
    }

    /**
     * @param name
     * @return a new declaration which is registered at types.
     * @throws ParseException
     *             if the declaration is already present
     */
    public static EnumType newDeclaration(TypeContext tc, Name name, String comment, List<Name> instances)
            throws ParseException {
        assert !instances.isEmpty() : "enums must have at least one instance";

        String skillName = name.getSkillName();
        if (tc.types.containsKey(skillName))
            throw new ParseException("Duplicate declaration of type " + name);

        EnumType rval = new EnumType(name, comment, instances);
        tc.types.put(skillName, rval);
        return rval;
    }

    public boolean isInitialized() {
        return null != fields;
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
    public void initialize(List<Field> Fields) throws ParseException {
        assert !isInitialized() : "multiple initialization";
        assert null != Fields : "no fields supplied";
        // check for duplicate fields
        {
            Set<String> names = new HashSet<>();
            for (Field f : Fields) {
                names.add(f.name);
                f.setDeclaredIn(this);
            }
            if (names.size() != Fields.size())
                throw new ParseException("Type " + name + " contains duplicate field definitions.");
        }

        this.fields = Fields;
    }

    /**
     * @return the fields added in this type
     */
    public List<Field> getFields() {
        assert isInitialized() : this.name + " has not been initialized";
        return fields;
    }

    /**
     * @return pretty parsable representation of this type
     */
    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("enum ").append(name.getSkillName());
        sb.append("{");
        for (Name i : instances) {
            sb.append(i).append(", ");
        }
        sb.append(";\n");
        for (Field f : fields)
            sb.append("\t").append(f.toString()).append("\n");
        sb.append("}");

        return sb.toString();
    }

    @Override
    public boolean isMonotone() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
