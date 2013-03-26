package de.ust.skill.ir;

import java.util.Stack;

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
		if ((l = (this instanceof GroundType))
				| (r = (o instanceof GroundType))) {
			return l == r ? 0 : (l ? -1 : 1);
		}
		// check for compound types
		if ((l = (this instanceof CompoundType))
				| (r = (o instanceof CompoundType))) {
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
	abstract public String toString();
	
	/**
	 * @return the type name, as used in a field declaration
	 */
	abstract public String getTypeName();
}
