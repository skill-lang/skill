package de.ust.skill.generator.haskell;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.ust.skill.ir.Field;
import de.ust.skill.ir.ReferenceType;
import de.ust.skill.ir.UserType;

public class CodeGenerator {

	private static List<UserType> IR;
	private static GeneralOutputMaker main;
	public static final String[] fileNames = { "Controls.hs", "Deserialize.hs", "Methods.hs", "ReadFields.hs",
			"Serialize.hs", "ImpossibleImports.hs", "Memory.hs", "Serialize.hs", "WriteFields.hs" };
	public static final boolean FOLLOW_REFERENCES = true;
	public static final boolean LEAVE_REFERENCES = false;

	public CodeGenerator(List<UserType> IR, GeneralOutputMaker main) {
		CodeGenerator.IR = IR;
		CodeGenerator.main = main; 
	}

	public void make() {
		Writer_Types.go(IR);
		Writer_Follow.go(IR);
		Writer_Access.go(IR);
		Writer_Interface.go(IR);
		copyStaticFiles();
	}

	public static void writeInFile(StringBuilder s, String fileName) {
		PrintWriter out = main.open(("/" + main.packagePath() + "/") + fileName);
		out.write(s.toString());
		out.close();
	}

	public static String escape(String string) {
		/*
		 * @TODO: IMPLEMENT ESCAPING HERE
		 */
		return string;
	}

	public static List<Field> getAllFields(UserType t) {
		List<Field> fields = new ArrayList<>();
		List<Field> temp;

		if (t.getSuperType() != null) {
			temp = getAllFields(t.getSuperType());
			fields.addAll(temp);
			temp = new ArrayList<>();
		}
		temp = t.getFields();
		fields.addAll(temp);
		temp = new ArrayList<>();

		return fields;
	}

	public static List<UserType> getTransitiveSubTypes(UserType type) {
		// requires a copy, not a reference, otherwise it has side effects
		List<UserType> subTypes = new LinkedList<>();
		subTypes.addAll(type.getSubTypes());

		List<UserType> justTransitiveSubTypes = new LinkedList<>();

		for (UserType subType : subTypes) {
			justTransitiveSubTypes.addAll(getTransitiveSubTypes(subType));
		}
		subTypes.addAll(justTransitiveSubTypes);
		return subTypes;
	}

	public static String somethingDataConstructor(UserType userType, Field field) {
		String fTN = field.getType().toString();
		
		if (fTN.startsWith("list") || fTN.startsWith("set") || fTN.startsWith("map") || fTN.endsWith("]")) {
			return "c'" + userType.getName().capital() + '_' + field.getName().capital();
		}
				
		return somethingDataConstructor(fTN);		
	}

	public static String somethingDataConstructor(String fTN) {
		switch (fTN.toLowerCase()) {
			case "bool":
				return "GBool";
			case "string":
				return "GString";
			case "i8":
				return "GInt8";
			case "i16":
				return "GInt16";
			case "i32":
				return "GInt32";
			case "i64":
				return "GInt64";
			case "v64":
				return "GV64";
			case "f32":
				return "GFloat";
			case "f64":
				return "GDouble";
			default:
				return "GRef";
		}
	}

	public static String getHaskellType(Field f, boolean refCommand) {
		String prefix = "";

		if (isReference(f)) {
			if (refCommand == FOLLOW_REFERENCES) {
				prefix = "Maybe ";	
			} else {
				return "Ref";
			}
		}

		String typeName = f.getType().toString().toUpperCase();
		String lowercase = typeName.toLowerCase();

		int l = lowercase.length();

		if (lowercase.startsWith("list")) {
			return '[' + baseToHaskell(typeName.substring(5, l - 1)) + ']';
		} else if (lowercase.startsWith("set")) {
			return '[' + baseToHaskell(typeName.substring(4, l - 1)) + ']';
		} else if (lowercase.startsWith("map")) {
			return "M.Map " + baseToHaskell(Methods.substring(typeName, 4, ',')) + ' '
					+ baseToHaskell(Methods.substring(typeName.substring(0, l - 1), ","));
		} else if (lowercase.endsWith("[]")) {
			return '[' + baseToHaskell(typeName.substring(0, l - 2)) + ']';
		} else if (lowercase.endsWith("]")) {
			return '[' + baseToHaskell(typeName.substring(0, l - 3)) + ']';
		} else {
			return prefix + baseToHaskell(typeName);
		}
	}

	public static boolean isReference(Field f) {
		return f.getType() instanceof ReferenceType && !f.getType().getName().getSkillName().equals("string");
	}

	private static String baseToHaskell(String string) {
		String lowercase = string.toLowerCase();

		switch (lowercase) {
			case "bool":
				return "Bool";
			case "string":
				return "String";
			case "i8":
				return "Int8";
			case "i16":
				return "Int16";
			case "i32":
				return "Int32";
			case "i64":
				return "Int64";
			case "v64":
				return "Int64";
			case "f32":
				return "Float";
			case "f64":
				return "Double";
			case "annotation":
				return "Pointer";
			default:
				return string;
		}
	}

	private static void copyStaticFiles() {
		final String inputPath = "deps/haskell/";
		final String outputPath = main.outPath() + "/generated" + ("/" + main.packagePath() + "/");

		new File(outputPath).mkdirs();

		try {
			for (String fileName : fileNames) {
				Files.copy(Paths.get(inputPath + fileName), Paths.get(outputPath + fileName),
						StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}