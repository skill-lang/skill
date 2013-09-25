package de.ust.skill.ir;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains the known types of a context. A context can be thought of as a
 * single front-end run.
 * 
 * @author Timm Felden
 */
public class TypeContext {
	protected final Map<String, Type> types = new HashMap<>();

	public TypeContext() {
		// two v64
		new GroundType(this, "annotation");

		new GroundType(this, "bool");

		new GroundType(this, "i8");
		new GroundType(this, "i16");
		new GroundType(this, "i32");
		new GroundType(this, "i64");

		new GroundType(this, "v64");

		new GroundType(this, "f32");
		new GroundType(this, "f64");

		new GroundType(this, "string");
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
	 *            the skill name of the type, i.e. no whitespace, only lowercase
	 *            letters
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
}
