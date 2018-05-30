/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Definition of a regular user type.
 * 
 * @author Timm Felden
 */
final public class UserType extends Declaration implements WithInheritance {

    /**
     * super type is the type above this type. base type is the base type of the formed type tree. This can even be
     * <i>this</i>.
     */
    private UserType superType = null, baseType = null;
    private final List<UserType> children = new ArrayList<>();

    // fields
    private List<Field> fields = null;
    private List<InterfaceType> interfaces;
    private List<View> views;
    private List<LanguageCustomization> customizations;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in pre-order over the type hierarchy.
     */
    private UserType(Name name, Comment comment, Collection<Restriction> restrictions, Collection<Hint> hints)
            throws ParseException {
        super(name, comment, restrictions, hints);

        superType = baseType = null;
    }

    @Override
    UserType copy(TypeContext tc) {
        try {
            return newDeclaration(tc, name, comment, restrictions, hints);
        } catch (ParseException e) {
            throw new Error("cannot happen", e);
        }
    }

    /**
     * @param name
     * @return a new declaration which is registered at types.
     * @throws ParseException
     *             if the declaration is already present
     */
    public static UserType newDeclaration(TypeContext tc, Name name, Comment comment,
            Collection<Restriction> restrictions, Collection<Hint> hints) throws ParseException {
        String skillName = name.getFqdn();
        if (tc.types.containsKey(skillName))
            throw new ParseException("Duplicate declaration of type " + name);

        UserType rval = new UserType(name, comment, restrictions, hints);
        tc.types.put(skillName, rval);
        return rval;
    }

    @Override
    public boolean isInitialized() {
        return null != baseType;
    }

    /**
     * Initializes the type declaration with data obtained from parsing the declarations body.
     * 
     * @param SuperType
     * @param Fields
     * @param interfaces
     * @throws ParseException
     *             thrown if the declaration is illegal, e.g. because it contains illegal hints
     */
    public void initialize(UserType SuperType, List<InterfaceType> interfaces, List<Field> Fields, List<View> views,
            List<LanguageCustomization> customizations) throws ParseException {
        assert !isInitialized() : "multiple initialization";
        assert null != Fields : "no fields supplied";

        if (null != SuperType) {
            assert null != SuperType.baseType : "types have to be initialized in pre-order";

            this.superType = SuperType;
            this.baseType = SuperType.baseType;
            SuperType.children.add(this);
        } else {
            baseType = this;
        }
        this.interfaces = interfaces;

        // check for duplicate fields
        {
            HashSet<Name> duplicates = new HashSet<>();
            HashSet<Name> seen = new HashSet<>();
            // add all field names visible from super types
            for (Declaration t : getAllSuperTypes()) {
                if (t instanceof WithFields) {
                    for (FieldLike f : ((WithFields) t).getAllFields()) {
                        seen.add(f.name);
                    }
                }
            }

            HashSet<Name> names = new HashSet<>();
            for (FieldLike f : Fields) {
                names.add(f.name);
                if (!seen.contains(f.name))
                    seen.add(f.name);
                else
                    duplicates.add(f.name);

                f.setDeclaredIn(this);
            }
            for (FieldLike f : views) {
                f.setDeclaredIn(this);
            }
            if (!duplicates.isEmpty()) {
                throw new ParseException("Type " + name + " contains duplicate field definitions: "
                        + Arrays.toString(duplicates.toArray(new Name[duplicates.size()])));
            }
        }

        this.fields = Fields;
        this.views = views;
        this.customizations = customizations;

        // check hints
        Hint.checkDeclaration(this, this.hints);
    }

    /*
     * (non-Javadoc)
     * @see de.ust.skill.ir.WithInheritance#getBaseType()
     */
    @Override
    public UserType getBaseType() {
        return baseType;
    }

    /*
     * (non-Javadoc)
     * @see de.ust.skill.ir.WithInheritance#getSuperType()
     */
    @Override
    public UserType getSuperType() {
        return superType;
    }

    /*
     * (non-Javadoc)
     * @see de.ust.skill.ir.WithInheritance#getSuperInterfaces()
     */
    @Override
    public List<InterfaceType> getSuperInterfaces() {
        return interfaces;
    }

    /*
     * (non-Javadoc)
     * @see de.ust.skill.ir.WithInheritance#getAllSuperTypes()
     */
    @Override
    public List<Declaration> getAllSuperTypes() {
        ArrayList<Declaration> rval = new ArrayList<Declaration>();
        rval.addAll(interfaces);
        if (null != superType)
            rval.add(superType);

        return rval;
    }

    public List<UserType> getSubTypes() {
        return children;
    }

    /*
     * (non-Javadoc)
     * @see de.ust.skill.ir.WithFields#getFields()
     */
    @Override
    public List<Field> getFields() {
        assert isInitialized() : this.name + " has not been initialized";
        return fields;
    }

    /**
     * @return all fields of an instance of the type, including fields declared in super types
     */
    @Override
    public List<Field> getAllFields() {
        assert isInitialized() : "you can not obtain fields of type " + name + " because it is not initialized";
        List<Field> rval = new ArrayList<>(fields);
        if (null != superType)
            rval.addAll(superType.getAllFields());

        for (InterfaceType i : interfaces) {
            List<Field> tmp = i.getAllFields();
            tmp.removeAll(rval);
            rval.addAll(tmp);
        }

        return rval;
    }

    /**
     * -1 -> false, 0 -> TBD, 1 -> true
     */
    private int distributedCached = 0;

    /**
     * cached query for distributed in any field. This is required to create a correct API in the presence of
     * distributed fields.
     * 
     * @return true, if the type or a super type has a distributed field
     */
    public boolean hasDistributedField() {
        if (0 == distributedCached) {
            if (null != superType && superType.hasDistributedField()) {
                distributedCached = 1;
            } else {
                distributedCached = -1;
                for (Field f : fields) {
                    if (f.isDistributed()) {
                        distributedCached = 1;
                        break;
                    }
                }
            }
        }

        return 1 == distributedCached;
    }

    /**
     * @return pretty parsable representation of this type
     */
    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder(name.getSkillName());
        if (null != superType) {
            sb.append(":").append(superType.name);
        }
        sb.append(" {\n");
        for (FieldLike f : fields)
            sb.append("\t").append(f.toString()).append(";\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public boolean isMonotone() {
        if (this == baseType)
            return hints.contains(Hint.monotone) || hints.contains(Hint.readonly);
        return baseType.isMonotone();
    }

    @Override
    public boolean isReadOnly() {
        if (this == baseType)
            return hints.contains(Hint.readonly);
        return baseType.isReadOnly();
    }

    @Override
    public List<View> getViews() {
        return views;
    }

    @Override
    public List<LanguageCustomization> getCustomizations() {
        return customizations;
    }
}
