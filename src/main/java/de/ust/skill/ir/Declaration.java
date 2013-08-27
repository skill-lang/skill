package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Declaration extends Type {

	private final String name;

	private Declaration superType = null, baseType = null;
	private List<Declaration> children = new ArrayList<>();
	/** once */
	private List<Field> fields = null;

	public Declaration(String name) {
		this.name = name;
		superType = baseType = null;
	}

	public String getName() {
		return name;
	}

	public void setParentType(Declaration p) {
		if (null == p)
			return;

		assert null == superType : "parent type already set";
		baseType = p.getBaseType();
		if (null == baseType)
			baseType = p;
		superType = p;
		superType.addChild(this);

		if (null == baseType)
			baseType = this;
		// update base type of all my children
		for (Declaration c : children)
			updateBaseType(c);

		if (this == baseType)
			baseType = null;
	}

	private static void updateBaseType(Declaration d) {
		d.baseType = d.superType.baseType;
		for (Declaration c : d.children)
			updateBaseType(c);
	}

	private void addChild(Declaration c) {
		children.add(c);
	}

	public Declaration getBaseType() {
		return baseType;
	}

	public Declaration getSuperType() {
		return superType;
	}

	/**
	 * can only be called once
	 */
	public void setFields(List<Field> fields) {
		assert null == this.fields : "Fields have already been assigned.";
		Collections.sort(fields);
		this.fields = Collections.unmodifiableList(fields);
	}

	public List<Field> getFields() {
		return fields;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(name);
		if (null != superType) {
			sb.append(":").append(superType.name);
		}
		sb.append("{");
		for (Field f : fields)
			sb.append("\t").append(f.toString()).append("\n");
		sb.append("}");

		return sb.toString();
	}

	@Override
	public String getTypeName() {
		return name;
	}
}
