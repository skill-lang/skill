package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of an interface type.
 * 
 * @author Timm Felden
 */
final public class InterfaceType extends Declaration {

    /**
     * super type is the type above this type. base type is the base type of the
     * formed type tree. Both are non null and may be annotations.
     */
    private Type superType = null;
    private Type baseType = null;

    // fields
    private List<Field> fields = null;
    private List<InterfaceType> superInterfaces;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact
     *             illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in
     *       pre-order over the type hierarchy.
     */
    private InterfaceType(Name name, Comment comment) throws ParseException {
        super(name, comment, Collections.<Restriction> emptyList(), Collections.<Hint> emptyList());

        superType = baseType = null;
    }

    /**
     * @param name
     * @return a new declaration which is registered at types.
     * @throws ParseException
     *             if the declaration is already present
     */
    public static InterfaceType newDeclaration(TypeContext tc, Name name, Comment comment) throws ParseException {
        String skillName = name.getSkillName();
        if (tc.types.containsKey(skillName))
            throw new ParseException("Duplicate declaration of type " + name);

        InterfaceType rval = new InterfaceType(name, comment);
        tc.types.put(skillName, rval);
        return rval;
    }

    @Override
    public boolean isInitialized() {
        return null != baseType;
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
    public void initialize(Type superType, List<InterfaceType> superInterfaces, List<Field> Fields)
            throws ParseException {
        this.superInterfaces = superInterfaces;
        assert !isInitialized() : "multiple initialization";
        assert null != Fields : "no fields supplied";
        // check for duplicate fields
        {
            Set<Name> names = new HashSet<>();
            for (Field f : Fields) {
                names.add(f.name);
                f.setDeclaredIn(this);
            }
            if (names.size() != Fields.size())
                throw new ParseException("Type " + name + " contains duplicate field definitions.");
        }

        this.superType = superType;
        if (superType instanceof UserType)
            baseType = ((UserType) superType).getBaseType();
        else
            baseType = superType;

        this.fields = Fields;
    }

    public Type getBaseType() {
        return baseType;
    }

    public Type getSuperType() {
        return superType;
    }

    /**
     * @return a list of super interfaces and the super type, if exists
     */
    public List<Declaration> getAllSuperTypes() {
        ArrayList<Declaration> rval = new ArrayList<Declaration>();
        rval.addAll(superInterfaces);
        if (superType instanceof UserType)
            rval.add((UserType) superType);

        return rval;
    }

    /**
     * @return the fields added in this type
     */
    public List<Field> getFields() {
        assert isInitialized() : this.name + " has not been initialized";
        return fields;
    }

    /**
     * @return all fields of an instance of the type, including fields declared
     *         in super types
     */
    public List<Field> getAllFields() {
        if (superType instanceof UserType) {
            List<Field> f = ((UserType) superType).getAllFields();
            f.addAll(fields);
            return f;
        }
        return new ArrayList<>(fields);
    }

    /**
     * @return pretty parsable representation of this type
     */
    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("interface ").append(name.getSkillName());
        if (null != superType) {
            sb.append(":").append(superType.getName());
        }
        for (InterfaceType i : superInterfaces) {
            sb.append(":").append(i.getName());
        }
        sb.append("{");
        for (Field f : fields)
            sb.append("\t").append(f.toString()).append("\n");
        sb.append("}");

        return sb.toString();
    }

    @Override
    public boolean isMonotone() {
        if (baseType instanceof UserType)
            return ((UserType) baseType).isMonotone();

        return false;
    }

    @Override
    public boolean isReadOnly() {
        if (baseType instanceof UserType)
            return ((UserType) baseType).isReadOnly();

        return false;
    }
}
