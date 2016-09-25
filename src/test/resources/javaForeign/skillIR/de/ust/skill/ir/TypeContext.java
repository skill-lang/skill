package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.ust.skill.ir.internal.EnumSubstitution;
import de.ust.skill.ir.internal.InterfaceSubstitution;
import de.ust.skill.ir.internal.Substitution;
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
	public static final class PointerType extends GroundType implements ReferenceType {
		PointerType(TypeContext tc, String name) {
			super(tc, name);
		}
	}

	public final Map<String, Type> types = new HashMap<>();
	/**
	 * all user declarations in type order
	 */
	public List<Declaration> declarations;
	public final List<UserType> usertypes = new ArrayList<>();
	public final List<InterfaceType> interfaces = new ArrayList<>();
	public final List<EnumType> enums = new ArrayList<>();
	public final List<Typedef> typedefs = new ArrayList<>();

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
	public final Type unifyType(Type t) {
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
	 * @return the set of all known (skill) type names, including built-in types
	 */
	public Set<String> allTypeNames() {
		return types.keySet();
	}

	/**
	 * called by the parser after initialization of all declarations
	 */
	public void setDefs(List<Declaration> orderedDeclarations) {
		assert null == declarations : "cannot initialize a context twice";

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

	/**
	 * @return an equivalent type context that is guaranteed not to have
	 *         interface types
	 * @note calling this function twice is efficient
	 * @note field definitions will be distributed to their implementers
	 * @note types will be replaced by the super type
	 * @todo move fields
	 */
	public TypeContext removeInterfaces() throws ParseException {
		if (interfaces.isEmpty())
			return this;

		return substitute(new InterfaceSubstitution());
		// TODO add fields that were removed from interfaces
	}

	/**
	 * The intended translation of an enum is to create an abstract class and a
	 * namespace for each enum type and to add singletons into that namespace
	 * for each instance of the enum.
	 * 
	 * @return an equivalent type context that is guaranteed not to have
	 *         interface types
	 * @note the resulting singletons will contain ':' characters in their name,
	 *       because name spaces did not find their way into the standard
	 */
	public TypeContext removeEnums() throws ParseException {
		if (enums.isEmpty())
			return this;

		return substitute(new EnumSubstitution(enums));
	}

	/**
	 * @return an equivalent type context that is guaranteed not to have
	 *         typedefs
	 * @note calling this function twice is efficient
	 */
	public TypeContext removeTypedefs() throws ParseException {
		if (typedefs.isEmpty())
			return this;

		return substitute(new TypedefSubstitution());
	}

	/**
	 * creates a clone of this applying the substitution σ
	 * 
	 * @note this is kind of a silver bullet implementation, because it is the
	 *       generalization of the three available substitutions, although they
	 *       are quite different; saves a lot of code though
	 */
	public TypeContext substitute(Substitution σ) throws ParseException {
		TypeContext tc = new TypeContext();
		List<Declaration> defs = new ArrayList<>(declarations.size());
		// copy types
		for (Declaration d : declarations)
			if (!σ.drop(d))
				defs.add(d.copy(tc));

		// append new types
		σ.addTypes(tc, defs);

		// initialize remaining types
		for (Declaration d : defs) {
			if (d instanceof UserType) {
				σ.initialize(this, tc, (UserType) d);

			} else if (d instanceof InterfaceType) {
				InterfaceType t = (InterfaceType) types.get(d.getSkillName());
				((InterfaceType) d).initialize(σ.substitute(tc, t.getSuperType()),
						substituteTypes(σ, tc, t.getSuperInterfaces()), substituteFields(σ, tc, t.getFields()),
						java.util.Collections.emptyList(), ((InterfaceType) d).getCustomizations());
			} else if (d instanceof EnumType) {
				EnumType t = (EnumType) types.get(d.getSkillName());
				((EnumType) d).initialize(substituteFields(σ, tc, t.getFields()), java.util.Collections.emptyList(),
						((EnumType) d).getCustomizations());
			} else {
				Typedef t = (Typedef) types.get(d.getSkillName());
				((Typedef) d).initialize(σ.substitute(tc, t.getTarget()));
			}
		}

		tc.setDefs(defs);

		return tc;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Declaration> List<T> substituteTypes(Substitution σ, TypeContext tc, List<T> ts)
			throws ParseException {
		List<T> rval = new ArrayList<>();
		for (T t : ts)
			if (!σ.drop(t))
				rval.add((T) σ.substitute(tc, t));
		return rval;
	}

	public static List<Field> substituteFields(Substitution σ, TypeContext tc, List<Field> fs) throws ParseException {
		List<Field> rval = new ArrayList<>();
		for (Field f : fs) {
			final Field sf = σ.substitute(tc, f);
			if (null != sf)
				rval.add(sf);
		}
		return rval;
	}

	/**
	 * @return a type context that is equivalent to this except that it contains
	 *         only user types
	 */
	public TypeContext removeSpecialDeclarations() throws ParseException {
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
