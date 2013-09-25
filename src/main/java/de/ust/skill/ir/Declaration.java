package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A declared user type.
 * 
 * @author Timm Felden
 */
final public class Declaration extends Type {

	// names
	private final String name;
	private final String skillName;
	private final String capitalName;

	/**
	 * super type is the type above this type. base type is the base type of the
	 * formed type tree. This can even be <i>this</i>.
	 */
	private Declaration superType = null, baseType = null;
	private final List<Declaration> children = new ArrayList<>();

	// TODO restrictions
	// TODO hints
	// TODO comments

	// fields
	private List<Field> fields = null;

	/**
	 * Creates a declaration of type name.
	 * 
	 * @note the declaration has to be completed, i.e. it has to be evaluated in
	 *       pre-order over the type hierarchy.
	 */
	private Declaration(String name) {
		this.name = name;
		this.skillName = name.toLowerCase();
		{
			char[] ch = name.toCharArray();
			ch[0] = Character.toUpperCase(ch[0]);
			this.capitalName = new String(ch);
		}

		superType = baseType = null;
	}

	/**
	 * @param name
	 * @return a new declaration which is registered at types.
	 * @throws ParseException
	 *             if the declaration is already present
	 */
	public static Declaration newDeclaration(TypeContext tc, String name) throws ParseException {
		String skillName = name.toLowerCase();
		if (tc.types.containsKey(skillName))
			throw new ParseException("Duplicate declaration of type " + name);

		Declaration rval = new Declaration(name);
		tc.types.put(skillName, rval);
		return rval;
	}

	/**
	 * Initializes the type declaration with data obtained from parsing the
	 * declarations body.
	 * 
	 * @param superType
	 * @param fields
	 * @throws ParseException
	 */
	@SuppressWarnings("hiding")
	public void initialize(Declaration superType, List<Field> fields) throws ParseException {
		assert null == this.fields : "multiple initialization";
		// check for duplicate fields
		{
			Set<String> names = new HashSet<>();
			for (Field f : fields)
				names.add(f.name);
			if (names.size() != fields.size())
				throw new ParseException("Type " + name + " contains duplicate field definitions.");
		}

		if (null != superType) {
			assert null != superType.baseType : "types have to be initialized in pre-order";

			this.superType = superType;
			this.baseType = superType.baseType;
			superType.children.add(this);
		} else {
			baseType = this;
		}

		this.fields = fields;
	}

	public Declaration getBaseType() {
		return baseType;
	}

	public Declaration getSuperType() {
		return superType;
	}

	/**
	 * @return the fields added in this type
	 */
	public List<Field> getFields() {
		return fields;
	}

	/**
	 * @return all fields of an instance of the type, including fields declared
	 *         in super types
	 */
	public List<Field> getAllFields() {
		if (null != superType) {
			List<Field> f = superType.getAllFields();
			f.addAll(fields);
			return f;
		}
		return new ArrayList<>(fields);
	}

	/**
	 * @return pretty parsable representation of this type
	 */
	public String prettyPrint() {
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
	public String getSkillName() {
		return skillName;
	}

	@Override
	public String getCapitalName() {
		return capitalName;
	}

	@Override
	public String getName() {
		return name;
	}
}
