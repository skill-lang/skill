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
public class EnumType extends Declaration implements WithFields {

    // fields
    public List<Field> fields = null;
    public List<Name> instances;
    public List<View> views;
    public List<LanguageCustomization> customizations;

    /**
     * Creates a declaration of type name.
     * 
     * @throws ParseException
     *             thrown, if the declaration to be constructed is in fact
     *             illegal
     * @note the declaration has to be completed, i.e. it has to be evaluated in
     *       pre-order over the type hierarchy.
     */
    public EnumType(Name name, Comment comment, List<Name> instances) throws ParseException {
        super(name, comment, Collections.<Restriction>emptyList(), Collections.<Hint>emptyList());
        this.instances = instances;
    }

    @Override
    EnumType copy(TypeContext tc) {
        try {
            return newDeclaration(tc, name, comment, instances);
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
    public static EnumType newDeclaration(TypeContext tc, Name name, Comment comment, List<Name> instances)
            throws ParseException {
        

        String skillName = name.getSkillName();
        if (tc.types.containsKey(skillName))
            throw new ParseException("Duplicate declaration of type " + name);

        EnumType rval = new EnumType(name, comment, instances);
        tc.types.put(skillName, rval);
        return rval;
    }

    @Override
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
    public void initialize(List<Field> Fields, List<View> views, List<LanguageCustomization> customizations)
            throws ParseException {
        
        
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

        this.fields = Fields;
        this.views = views;
        this.customizations = customizations;
    }

    /**
     * @return the fields added in this type
     */
    @Override
    public List<Field> getFields() {
        
        return fields;
    }

    /**
     * @return pretty parsable representation of this type
     */
    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("enum ").append(name.getSkillName());
        sb.append("{");
        for (Name i : getInstances()) {
            sb.append(i).append(", ");
        }
        sb.append(";\n");
        for (FieldLike f : fields)
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

    public List<Name> getInstances() {
        return instances;
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
