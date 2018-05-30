/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
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
final public class InterfaceType extends Declaration implements WithInheritance {

    /**
     * super type is the type above this type. base type is the base type of the formed type tree. Both are non null and
     * may be annotations.
     */
    private Type superType = null;
    private Type baseType = null;

    // fields
    private List<Field> fields = null;
    private List<InterfaceType> superInterfaces;
    private List<View> views;
    private List<LanguageCustomization> customizations;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in pre-order over the type hierarchy.
     */
    private InterfaceType(Name name, Comment comment) throws ParseException {
        super(name, comment, Collections.<Restriction>emptyList(), Collections.<Hint>emptyList());

        superType = baseType = null;
    }

    @Override
    InterfaceType copy(TypeContext tc) {
        try {
            return newDeclaration(tc, name, comment);
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
     * Initializes the type declaration with data obtained from parsing the declarations body.
     * 
     * @param SuperType
     * @param Fields
     * @throws ParseException
     *             thrown if the declaration is illegal, e.g. because it contains illegal hints
     */
    public void initialize(Type superType, List<InterfaceType> superInterfaces, List<Field> Fields, List<View> views,
            List<LanguageCustomization> customizations) throws ParseException {
        this.superInterfaces = superInterfaces;
        assert !isInitialized() : "multiple initialization";
        assert null != Fields : "no fields supplied";
        // check for duplicate fields
        {
            Set<Name> names = new HashSet<>();
            for (FieldLike f : Fields) {
                names.add(f.name);
                f.setDeclaredIn(this);
            }
            for (FieldLike f : views) {
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
        this.views = views;
        this.customizations = customizations;
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public Type getSuperType() {
        return superType;
    }

    /**
     * @return a list of super interfaces and the super type, if exists
     */
    @Override
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
        List<Field> rval = new ArrayList<>(fields);
        if (superType instanceof UserType)
            rval.addAll(((UserType) superType).getAllFields());

        for (InterfaceType i : superInterfaces) {
            List<Field> tmp = i.getAllFields();
            tmp.removeAll(rval);
            rval.addAll(tmp);
        }

        return rval;
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
        for (FieldLike f : fields)
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

    @Override
    public List<InterfaceType> getSuperInterfaces() {
        return superInterfaces;
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
