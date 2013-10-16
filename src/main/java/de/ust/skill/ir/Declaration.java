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
final public class Declaration extends Type implements ReferenceType {

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

	/**
	 * The restrictions applying to this declaration.
	 */
	private final List<Restriction> restrictions;
	// TODO hints
	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 */
	private final String skillCommentImage;

	// fields
	private List<Field> fields = null;

	/**
	 * Creates a declaration of type name.
	 * 
	 * @note the declaration has to be completed, i.e. it has to be evaluated in
	 *       pre-order over the type hierarchy.
	 */
	private Declaration(String name, String comment, List<Restriction> restrictions) {
		this.name = name;
		this.skillName = name.toLowerCase();
		{
			char[] ch = name.toCharArray();
			ch[0] = Character.toUpperCase(ch[0]);
			this.capitalName = new String(ch);
		}
		skillCommentImage = null == comment ? "" : comment;
		this.restrictions = restrictions;

		superType = baseType = null;
	}

	/**
	 * @param name
	 * @return a new declaration which is registered at types.
	 * @throws ParseException
	 *             if the declaration is already present
	 */
	public static Declaration newDeclaration(TypeContext tc, String name, String comment, List<Restriction> restrictions)
			throws ParseException {
		String skillName = name.toLowerCase();
		if (tc.types.containsKey(skillName))
			throw new ParseException("Duplicate declaration of type " + name);

		Declaration rval = new Declaration(name, comment, restrictions);
		tc.types.put(skillName, rval);
		return rval;
	}

	public boolean isInitialized() {
		return null != baseType;
	}

	/**
	 * Initializes the type declaration with data obtained from parsing the
	 * declarations body.
	 * 
	 * @param SuperType
	 * @param Fields
	 * @throws ParseException
	 */
	public void initialize(Declaration SuperType, List<Field> Fields) throws ParseException {
		assert !isInitialized() : "multiple initialization";
		assert null != Fields : "no fields supplied";
		// check for duplicate fields
		{
			Set<String> names = new HashSet<>();
			for (Field f : Fields)
				names.add(f.name);
			if (names.size() != Fields.size())
				throw new ParseException("Type " + name + " contains duplicate field definitions.");
		}

		if (null != SuperType) {
			assert null != SuperType.baseType : "types have to be initialized in pre-order";

			this.superType = SuperType;
			this.baseType = SuperType.baseType;
			SuperType.children.add(this);
		} else {
			baseType = this;
		}

		this.fields = Fields;
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
		assert isInitialized() : this.name + " has not been initialized";
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

	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 * 
	 * This may require further transformation depending on the target language.
	 * 
	 * @note can contain newline characters!!!
	 */
	public String getSkillComment() {
		return skillCommentImage;
	}

	public List<Restriction> getRestrictions() {
		return restrictions;
	}
}
