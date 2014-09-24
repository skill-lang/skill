package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.ust.skill.ir.internal.TypedefSubstitution;

/**
 * The Type Context corresponding to a given SKilL specification. The context
 * provides information about sub types, parent user types, implemented
 * interfaces, and so on. Furthermore, the context can be used to remove certain
 * kinds of types from the list of types.
 * 
 * @todo cache remove operations to ease code generation
 * @author Timm Felden
 */
public final class TypeContext {
    /**
     * Mark ground types which create reference like fields.
     * 
     * @author Timm Felden
     */
    private static final class PointerType extends GroundType implements ReferenceType {
        PointerType(TypeContext tc, String name) {
            super(tc, name);
        }
    }

    protected final Map<String, Type> types = new HashMap<>();
    /**
     * all user declarations in type order
     */
    private List<Declaration> declarations;
    private final List<UserType> usertypes = new ArrayList<>();
    private final List<InterfaceType> interfaces = new ArrayList<>();
    private final List<EnumType> enums = new ArrayList<>();
    private final List<Typedef> typedefs = new ArrayList<>();

    public TypeContext() {
        new PointerType(this, "annotation");

        new GroundType(this, "bool");

        new GroundType(this, "i8");
        new GroundType(this, "i16");
        new GroundType(this, "i32");
        new GroundType(this, "i64");

        new GroundType(this, "v64");

        new GroundType(this, "f32");
        new GroundType(this, "f64");

        new PointerType(this, "string");
    }

    /**
     * unification has to be done in the constructor or a factory method and
     * must not be made visible to a client
     * 
     * @param t
     *            the type to be unified
     * @return t, if t does not yet exist or the existing instance of the type
     */
    protected final Type unifyType(Type t) {
        if (types.containsKey(t.getSkillName()))
            return types.get(t.getSkillName());

        types.put(t.getSkillName(), t);
        return t;
    }

    /**
     * @param skillName
     *            the skill name of the type, i.e. no whitespace, only lower
     *            case letters
     * @return the respective type, if it exists, null otherwise
     * @throws ParseException
     *             if the argument type name is not the name of a known type.
     */
    public Type get(String skillName) throws ParseException {
        if (!types.containsKey(skillName))
            throw new ParseException("Type " + skillName + " is not a known type.");
        return types.get(skillName);
    }

    /**
     * @return the set of all known (skill) type names
     */
    public Set<String> allTypeNames() {
        return types.keySet();
    }

    /**
     * called by the parser after initialization of all declarations
     */
    public void setDefs(List<Declaration> orderedDeclarations) {
        assert null == declarations : "can not initialize a context twice";

        declarations = orderedDeclarations;
        for (Declaration d : declarations) {
            if (d instanceof UserType)
                usertypes.add((UserType) d);
            else if (d instanceof InterfaceType)
                interfaces.add((InterfaceType) d);
            else if (d instanceof EnumType)
                enums.add((EnumType) d);
            else
                typedefs.add((Typedef) d);

        }
    }

    public TypeContext removeInterfaces() {
        throw new Error();
        // TODO implementation! π_i
    }

    public TypeContext removeEnums() {
        throw new Error();
        // TODO implementation! π_e
    }

    /**
     * @return an equivalent type context that is guaranteed not to have
     *         typedefs
     * @note calling this function twice is efficient
     */
    public TypeContext removeTypedefs() {
        if (typedefs.isEmpty())
            return this;

        return substitute(new TypedefSubstitution());
    }

    /**
     * creates a clone of this applying the substitution σ
     */
    private TypeContext substitute(TypedefSubstitution σ) {
        TypeContext tc = new TypeContext();
        List<Declaration> defs = new ArrayList<>(declarations.size());
        // copy types
        for (Declaration d : declarations)
            if (!σ.drop(d))
                defs.add(d.copy(tc));

        // initialize remaining types
        try {
            for (Declaration d : defs) {
                if (d instanceof UserType)
                    ((UserType) d).initialize((UserType) σ.substitute(tc, ((UserType) d).getSuperType()),
                            substituteTypes(σ, tc, ((UserType) d).getSuperInterfaces()),
                            substituteFields(σ, tc, ((WithFields) d).getFields()));

                else if (d instanceof InterfaceType)
                    ((InterfaceType) d).initialize(σ.substitute(tc, ((InterfaceType) d).getSuperType()),
                            substituteTypes(σ, tc, ((InterfaceType) d).getSuperInterfaces()),
                            substituteFields(σ, tc, ((WithFields) d).getFields()));
                else if (d instanceof EnumType)
                    ((EnumType) d).initialize(substituteFields(σ, tc, ((WithFields) d).getFields()));
                else
                    ((Typedef) d).initialize(σ.substitute(tc, ((Typedef) d).getTarget()));
            }
        } catch (ParseException e) {
            throw new Error("can not happen", e);
        }

        tc.setDefs(defs);

        return tc;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Declaration> List<T> substituteTypes(TypedefSubstitution σ, TypeContext tc, List<T> ts)
            throws ParseException {
        List<T> rval = new ArrayList<>();
        for (T t : ts)
            rval.add((T) σ.substitute(tc, t));
        return rval;
    }

    private static List<Field> substituteFields(TypedefSubstitution σ, TypeContext tc, List<Field> fs) {
        List<Field> rval = new ArrayList<>();
        for (Field f : fs)
            rval.add(σ.substitute(tc, f));
        return rval;
    }

    /**
     * @return a type context that is equivalent to this except that it contains
     *         only user types
     */
    public TypeContext removeSpecialDeclarations() {
        return removeTypedefs().removeInterfaces().removeEnums();
    }

    public List<UserType> getUsertypes() {
        return usertypes;
    }

    public List<InterfaceType> getInterfaces() {
        return interfaces;
    }

    public List<EnumType> getEnums() {
        return enums;
    }

    public List<Typedef> getTypedefs() {
        return typedefs;
    }
}
