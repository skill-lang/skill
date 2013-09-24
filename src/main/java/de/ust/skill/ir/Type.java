package de.ust.skill.ir;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Base of the Type hierarchy.
 * 
 * Types are comparable and unique. Uniqueness is achieved using a hash tree
 * over the skill name.
 * 
 * The order on types uses type order, i.e. super types are *lower* then sub
 * types. Other parts of the order are left under specified, although the
 * implemented order is total.
 * 
 * @author Timm Felden
 */
public abstract class Type implements Comparable<Type> {
	protected static final Map<String, Type> types = new HashMap<>();

	/**
	 * unification has to be done in the constructor or a factory method and
	 * must not be made visible to a client
	 * 
	 * @param t
	 *            the type to be unified
	 * @return t, if t does not yet exist or the existing instance of the type
	 */
	protected static final Type unifyType(Type t) {
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
	 */
	public static Type get(String skillName) {
		return types.get(skillName);
	}

	/**
	 * @return the set of all known (skill) type names
	 */
	public static Set<String> allTypeNames() {
		return types.keySet();
	}

	/**
	 * Implements the type order as defined in the paper.
	 */
	@Override
	final public int compareTo(Type o) {
		// it is necessary, that we can assume this!=o later on
		if (this == o)
			return 0;

		boolean r, l;
		// check for ground types
		if ((l = (this instanceof GroundType)) | (r = (o instanceof GroundType))) {
			return l == r ? 0 : (l ? -1 : 1);
		}
		// check for compound types
		if ((l = (this instanceof ContainerType)) | (r = (o instanceof ContainerType))) {
			return l == r ? 0 : (l ? -1 : 1);
		}

		// we have to compare declarations
		Declaration t = (Declaration) this, s = (Declaration) o;

		if ((l = (t.getBaseType() == null)) | (r = (s.getBaseType() == null))) {
			if (l && r)
				return t.getName().compareTo(s.getName());

			// the base type is smaller
			return s.getBaseType() == t ? -1 : 1;
		}

		if (t.getBaseType() == s.getBaseType()) {
			Stack<Declaration> left = new Stack<>(), right = new Stack<>();
			// collect super types
			for (Declaration i = t; i != null; i = i.getSuperType())
				left.push(i);
			for (Declaration i = s; i != null; i = i.getSuperType())
				right.push(i);

			// pop common super classes (can not be null, because this is not
			// equal)
			while (left.peek() == right.peek()) {
				left.pop();
				right.pop();
			}

			if (left.isEmpty())
				return -1;
			if (right.isEmpty())
				return 1;

			return left.peek().getName().compareTo(right.peek().getName());
		}

		return t.getBaseType().getName().compareTo(s.getBaseType().getName());
	}

	@Override
	final public String toString() {
		return getSkillName();
	}

	/**
	 * @return the types name, as used in a binary skill file; if the type is
	 *         not serialized using strings, this result is the lower case
	 *         space-free representation as it may occur in a skill
	 *         specification file.
	 */
	abstract public String getSkillName();

	/**
	 * @return the name of the type as it occurred in the declaration; for
	 *         built-in types, this is equal to the skill name
	 */
	abstract public String getName();

	/**
	 * @return returns the type with a capitalized first letter
	 */
	abstract public String getCapitalName();
}
