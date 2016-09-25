package de.ust.skill.ir;

import java.util.Stack;

/**
 * Base of the Type hierarchy.
 * 
 * Types are comparable and unique. Uniqueness is guaranteed by the type
 * context.
 * 
 * The order on types uses type order, i.e. super types are *lower* then sub
 * types. Other parts of the order are left under specified, although the
 * implemented order is total.
 * 
 * @author Timm Felden
 */
public abstract class Type implements Comparable<Type> {

	/**
	 * Implements the type order as defined in the paper.
	 */
	@Override
	public int compareTo(Type o) {
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
		UserType t = (UserType) this, s = (UserType) o;

		if ((l = (t.getBaseType() == null)) | (r = (s.getBaseType() == null))) {
			if (l && r)
				return t.getName().compareTo(s.getName());

			// the base type is smaller
			return s.getBaseType() == t ? -1 : 1;
		}

		if (t.getBaseType() == s.getBaseType()) {
			Stack<UserType> left = new Stack<>(), right = new Stack<>();
			// collect super types
			for (UserType i = t; i != null; i = i.getSuperType())
				left.push(i);
			for (UserType i = s; i != null; i = i.getSuperType())
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
	public String toString() {
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
    abstract public Name getName();
}
