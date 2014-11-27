package de.ust.skill.generator.c;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ust.skill.ir.ConstantLengthArrayType;
import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Declaration;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.GroundType;
import de.ust.skill.ir.ListType;
import de.ust.skill.ir.MapType;
import de.ust.skill.ir.SetType;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.VariableLengthArrayType;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Generator {

	private final String outPath;

	// The prefix used for code of the generated binding
	private final String prefix;
	private final Configuration configuration;
	private final List<Declaration> declarations;
	private String header = "";

	protected Map<String, Object> rootMap;

	public Generator(String outPath, String prefix,
			List<Declaration> declarations, boolean safe) {

		this.outPath = outPath;
		if (!prefix.equals("")) {
			if (!prefix.endsWith("_")) {
				prefix = prefix.concat("_");
			}
		}
		this.prefix = prefix;
		this.declarations = declarations;

		configuration = new Configuration();

		// Specify the data source where the template files come from.
		try {
			configuration
					.setDirectoryForTemplateLoading(new File("templates/c/"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		configuration.setObjectWrapper(new DefaultObjectWrapper());
		configuration.setDefaultEncoding("UTF-8");

		configuration
				.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		this.createRootMap(declarations, safe);
	}

	public String getOutPath() {
		return outPath;
	}

	public List<Declaration> getDeclarations() {
		return declarations;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void make() throws Exception {

		/**
		 * api
		 */
		String outPathApi = outPath + File.separator + "api";
		writeFile(outPathApi + File.separator + prefix + "api.h", "api-h.ftl",
				prefix);
		writeFile(outPathApi + File.separator + prefix + "api.c", "api-c.ftl",
				prefix);

		/**
		 * io
		 */
		String outPathIo = outPath + File.separator + "io";
		writeFile(outPathIo + File.separator + prefix + "reader.h",
				"reader-h.ftl", prefix);
		writeFile(outPathIo + File.separator + prefix + "reader.c",
				"reader-c.ftl", prefix);

		writeFile(outPathIo + File.separator + prefix + "writer.h",
				"writer-h.ftl", prefix);
		writeFile(outPathIo + File.separator + prefix + "writer.c",
				"writer-c.ftl", prefix);

		writeFile(outPathIo + File.separator + prefix + "binary_reader.h",
				"binary-reader-h.ftl", prefix);
		writeFile(outPathIo + File.separator + prefix + "binary_reader.c",
				"binary-reader-c.ftl", prefix);

		writeFile(outPathIo + File.separator + prefix + "binary_writer.h",
				"binary-writer-h.ftl", prefix);
		writeFile(outPathIo + File.separator + prefix + "binary_writer.c",
				"binary-writer-c.ftl", prefix);

		/**
		 * model
		 */
		String outPathModel = outPath + File.separator + "model";
		writeFile(outPathModel + File.separator + prefix + "string_access.h",
				"string-access-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix + "string_access.c",
				"string-access-c.ftl", prefix);

		writeFile(outPathModel + File.separator + prefix + "skill_state.h",
				"skill-state-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix + "skill_state.c",
				"skill-state-c.ftl", prefix);

		writeFile(
				outPathModel + File.separator + prefix + "type_information.h",
				"type-information-h.ftl", prefix);
		writeFile(
				outPathModel + File.separator + prefix + "type_information.c",
				"type-information-c.ftl", prefix);

		writeFile(
				outPathModel + File.separator + prefix + "type_declaration.h",
				"type-declaration-h.ftl", prefix);
		writeFile(
				outPathModel + File.separator + prefix + "type_declaration.c",
				"type-declaration-c.ftl", prefix);

		writeFile(outPathModel + File.separator + prefix + "type_enum.h",
				"type-enum-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix + "type_enum.c",
				"type-enum-c.ftl", prefix);

		writeFile(outPathModel + File.separator + prefix + "storage_pool.h",
				"storage-pool-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix + "storage_pool.c",
				"storage-pool-c.ftl", prefix);

		writeFile(outPathModel + File.separator + prefix
				+ "field_information.h", "field-information-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix
				+ "field_information.c", "field-information-c.ftl", prefix);

		writeFile(outPathModel + File.separator + prefix + "types.h",
				"types-h.ftl", prefix);
		writeFile(outPathModel + File.separator + prefix + "types.c",
				"types-c.ftl", prefix);

		/**
		 * makefile
		 */
		String outPathMakefile = outPath;
		writeFile(outPathMakefile + File.separator + "makefile",
				"makefile.ftl", prefix);
	}

	private void setTypeFields(Map<String, Object> map, Type source,
			boolean isConstant) {
		String enumType = typeToEnumType(source);
		if (isConstant) {
			enumType = "CONSTANT_" + enumType;
		}
		map.put("enum_type", this.prefix + enumType);
		if (source instanceof Declaration) {
			map.put("declaration_type_name", ((Declaration) source).getName()
					.toLowerCase());
		}
		map.put("c_type", this.typeToCType(source));
		map.put("read_method", this.typeToReadMethod(source));
		map.put("write_method", this.typeToWriteMethod(source));
		map.put("is_declaration", source instanceof Declaration);
		map.put("is_ground_type", source instanceof GroundType);
		map.put("is_container_type", source instanceof ContainerType);
		map.put("is_constant_length_array",
				source instanceof ConstantLengthArrayType);
		map.put("is_variable_length_array",
				source instanceof VariableLengthArrayType);
		map.put("is_set_type", source instanceof SetType);
		map.put("is_list_type", source instanceof ListType);
		map.put("is_map", source instanceof MapType);

		map.put("is_string", source.getName().equals("string"));
		map.put("is_annotation", source.getName().equals("annotation"));

		if (source instanceof ContainerType) {
			if (source instanceof MapType) {
				List<Type> baseTypes = ((MapType) source).getBaseTypes();
				List<Map<String, Object>> baseTypesList = new ArrayList<>();
				// The last baseType is used as value-type when handling the
				// previous base type, thus we don't need it in this list.
				for (int i = 0; i < baseTypes.size() - 1; i++) {
					Type currentType = baseTypes.get(i);
					Map<String, Object> baseType = new HashMap<>();
					setTypeFields(baseType, currentType, false);
					// We need to know the position in the base-types list, as
					// for maps with more than
					// two base-types, we have to set maps as values of a map.
					baseType.put("base_type_index", i);
					if (baseTypes.size() - i > 2) {
						// The values of this map are maps. To read or write
						// these maps, the binding will use an extra method.
						baseType.put("has_nested_map", true);
					} else if (baseTypes.size() - i == 2) {
						// We have a simple key-value map here, so store the
						// value-type as well.
						baseType.put("has_nested_map", false);
						Map<String, Object> mapValueType = new HashMap<>();
						setTypeFields(mapValueType, baseTypes.get(i + 1), false);
						baseType.put("map_value_type", mapValueType);
					}
					baseTypesList.add(baseType);
				}
				map.put("base_types", baseTypesList);
				map.put("base_types_length", baseTypes.size());
			} else {
				Map<String, Object> baseType = new HashMap<>();
				if (source instanceof ConstantLengthArrayType) {
					ConstantLengthArrayType typeCasted = (ConstantLengthArrayType) source;
					setTypeFields(baseType, typeCasted.getBaseType(), false);
					map.put("array_length", typeCasted.getLength());
				} else if (source instanceof ListType) {
					ListType typeCasted = (ListType) source;
					setTypeFields(baseType, typeCasted.getBaseType(), false);
				} else if (source instanceof VariableLengthArrayType) {
					VariableLengthArrayType typeCasted = (VariableLengthArrayType) source;
					setTypeFields(baseType, typeCasted.getBaseType(), false);
				} else if (source instanceof SetType) {
					SetType typeCasted = (SetType) source;
					setTypeFields(baseType, typeCasted.getBaseType(), false);
				}
				map.put("base_type", baseType);
			}
		}
	}

	private Map<String, Object> createFieldMap(Field source,
			Declaration declaration) {
		Map<String, Object> result = new HashMap<>();
		result.put("name", source.getName().toLowerCase());
		result.put("is_transient", source.isAuto());
		result.put("is_constant", source.isConstant());
		result.put("constant_value", source.constantValue());
		result.put("declaration_name", declaration.getName().toLowerCase());
		result.put("contains_strings", containsStringFields(source));
		setTypeFields(result, source.getType(), source.isConstant());
		return result;
	}

	// This does not include fields inherited from supertypes and no constant
	// fields
	private List<Map<String, Object>> createFieldsList(Declaration source) {
		List<Map<String, Object>> result = new ArrayList<>();

		for (Field currentField : source.getFields()) {
			if (!currentField.isConstant()) {
				result.add(createFieldMap(currentField, source));
			}
		}
		return result;
	}

	// This does not include fields inherited from supertypes.
	private List<Map<String, Object>> createConstantFieldsList(
			Declaration source) {
		List<Map<String, Object>> result = new ArrayList<>();

		for (Field currentField : source.getFields()) {
			result.add(createFieldMap(currentField, source));
		}
		return result;
	}

	private boolean containsStringFields(Field field) {
		if (field.isAuto()) {
			return false;
		}
		if (field.getType().getName().equals("string")) {
			return true;
		}
		Type type = field.getType();
		if (type instanceof ContainerType) {
			if (type instanceof ConstantLengthArrayType) {
				if (((ConstantLengthArrayType) type).getBaseType().getName()
						.equals("string")) {
					return true;
				}
			} else if (type instanceof VariableLengthArrayType) {
				if (((VariableLengthArrayType) type).getBaseType().getName()
						.equals("string")) {
					return true;
				}
			} else if (type instanceof ListType) {
				if (((ListType) type).getBaseType().getName().equals("string")) {
					return true;
				}
			} else if (type instanceof SetType) {
				if (((SetType) type).getBaseType().getName().equals("string")) {
					return true;
				}
			} else if (type instanceof MapType) {
				for (Type currentBaseType : ((MapType) type).getBaseTypes()) {
					if (currentBaseType.getName().equals("string")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// This is used to avoid unnecessary code in the binding.
	private boolean containsStringFields(Declaration declaration) {
		for (Field currentField : declaration.getAllFields()) {
			if (containsStringFields(currentField)) {
				return true;
			}
		}
		return false;
	}

	private void createRootMap(List<Declaration> declarations, boolean safe) {

		this.rootMap = new HashMap<>();
		rootMap.put("prefix", getPrefix());
		rootMap.put("prefix_capital", getPrefix().toUpperCase());
		rootMap.put("safe", safe);

		// Those are optimizations to avoid unused code in the generated binding
		boolean containsStringFields = false;
		for (Declaration currentDeclaration : declarations) {
			if (containsStringFields(currentDeclaration)) {
				containsStringFields = true;
			}
		}
		rootMap.put("contains_string_fields", containsStringFields);

		List<Map<String, Object>> declarationsList = new ArrayList<>();
		for (Declaration currentDeclaration : declarations) {
			Map<String, Object> declarationMap = new HashMap<>();
			declarationMap.put("name", currentDeclaration.getName()
					.toLowerCase());

			Type superType = currentDeclaration.getSuperType();
			if (superType == null) {
				declarationMap.put("super_type", "skill_type");
			} else {
				declarationMap.put("super_type", superType.getName()
						.toLowerCase());
			}

			// contains only fields of this exact type, no fields inherited from
			// super types, and no constant fields
			List<Map<String, Object>> fieldsList = createFieldsList(currentDeclaration);
			declarationMap.put("fields", fieldsList);
			declarationMap.put("contains_strings",
					containsStringFields(currentDeclaration));

			// contains only fields of this exact type, no fields inherited from
			// super types, and also including constant fields
			List<Map<String, Object>> constantFieldsList = createConstantFieldsList(currentDeclaration);
			declarationMap
					.put("fields_including_constants", constantFieldsList);

			// contains all fields inherited from super types, but no constant
			// fields
			List<Map<String, Object>> superFieldsList = new ArrayList<>();
			Declaration superDeclaration = currentDeclaration.getSuperType();
			while (superDeclaration != null) {
				superFieldsList.addAll(createFieldsList(superDeclaration));
				superDeclaration = superDeclaration.getSuperType();
			}
			declarationMap.put("super_fields", superFieldsList);

			// contains all fields inherited from super types
			List<Map<String, Object>> constantSuperFieldsList = new ArrayList<>();
			Declaration superConstantDeclaration = currentDeclaration
					.getSuperType();
			while (superConstantDeclaration != null) {
				constantSuperFieldsList
						.addAll(createConstantFieldsList(superConstantDeclaration));
				superConstantDeclaration = superConstantDeclaration
						.getSuperType();
			}
			declarationMap.put("super_fields_including_constants",
					constantSuperFieldsList);

			// Contains all fields of this type, including fields of all super
			// types
			List<Map<String, Object>> allFieldsList = new ArrayList<>();
			allFieldsList.addAll(fieldsList);
			allFieldsList.addAll(superFieldsList);
			declarationMap.put("all_fields", allFieldsList);

			List<Map<String, Object>> allConstantFieldsList = new ArrayList<>();
			allConstantFieldsList.addAll(constantFieldsList);
			allConstantFieldsList.addAll(constantSuperFieldsList);
			declarationMap.put("all_fields_including_constants",
					allConstantFieldsList);

			declarationsList.add(declarationMap);
		}
		rootMap.put("declarations", declarationsList);
	}

	private void writeFile(String outFile, String templateName,
			String packagePrefix) throws IOException, TemplateException {
		Template template = getFreemarkerConfiguration().getTemplate(
				templateName);
		Writer fileWriter = createPrintWriter(outFile);
		template.process(getRootMap(), fileWriter);
	}

	private Configuration getFreemarkerConfiguration() {
		return this.configuration;
	}

	private Map<String, Object> getRootMap() {
		return this.rootMap;
	}

	private String typeToCType(Type type) {
		if (type instanceof GroundType) {
			switch (type.getName()) {
			case "annotation":
				return this.prefix + "skill_type";
			case "bool":
				return "bool";

			case "i8":
				return "int8_t";
			case "i16":
				return "int16_t";
			case "i32":
				return "int32_t";
			case "i64":
				return "int64_t";
			case "v64":
				return "int64_t";

			case "f32":
				return "float";
			case "f64":
				return "double";

			case "string":
				return "char";
			}
		} else if (type instanceof ConstantLengthArrayType) {
			return "GArray";
		} else if (type instanceof VariableLengthArrayType) {
			return "GArray";
		} else if (type instanceof ListType) {
			return "GList";
		} else if (type instanceof SetType) {
			return "GHashTable";
		} else if (type instanceof MapType) {
			return "GHashTable";
		} else if (type instanceof Declaration) {
			return this.prefix + ((Declaration) type).getName().toLowerCase();
		} else {
			return "unknown type";
		}
		return null;
	}

	private String typeToReadMethod(Type type) {
		if (type instanceof GroundType) {
			switch (type.getName()) {
			case "bool":
				return this.prefix + "read_bool";

			case "i8":
				return this.prefix + "read_i8";
			case "i16":
				return this.prefix + "read_i16";
			case "i32":
				return this.prefix + "read_i32";
			case "i64":
				return this.prefix + "read_i64";
			case "v64":
				return this.prefix + "read_v64";

			case "f32":
				return this.prefix + "read_f32";
			case "f64":
				return this.prefix + "read_f64";
			}
		}
		return null;
	}

	private String typeToWriteMethod(Type type) {
		if (type instanceof GroundType) {
			switch (type.getName()) {
			case "bool":
				return this.prefix + "write_bool";

			case "i8":
				return this.prefix + "write_i8";
			case "i16":
				return this.prefix + "write_i16";
			case "i32":
				return this.prefix + "write_i32";
			case "i64":
				return this.prefix + "write_i64";
			case "v64":
				return this.prefix + "write_v64";

			case "f32":
				return this.prefix + "write_f32";
			case "f64":
				return this.prefix + "write_f64";
			}
		}
		return null;
	}

	private String typeToEnumType(Type type) {
		if (type instanceof ConstantLengthArrayType) {
			return "CONSTANT_LENGTH_ARRAY";
		} else if (type instanceof VariableLengthArrayType) {
			return "VARIABLE_LENGTH_ARRAY";
		} else if (type instanceof ListType) {
			return "LIST";
		} else if (type instanceof SetType) {
			return "SET";
		} else if (type instanceof MapType) {
			return "MAP";
		} else if (type instanceof Declaration) {
			return "USER_TYPE";
		} else if (type instanceof GroundType) {
			switch (type.getName()) {
			case "bool":
				return "BOOLEAN";
			default:
				return type.getName().toUpperCase();
			}
		}
		return null;
	}

	private PrintWriter createPrintWriter(String fileName) throws IOException {
		File target = new File(fileName);
		target.getParentFile().mkdirs();
		target.createNewFile();
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		return writer;
	}
}
