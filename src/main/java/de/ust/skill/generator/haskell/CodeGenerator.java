package de.ust.skill.generator.haskell;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import de.ust.skill.ir.Field;
import de.ust.skill.ir.Name;
import de.ust.skill.ir.ReferenceType;
import de.ust.skill.ir.UserType;

public class CodeGenerator {

	private final List<UserType>		IR;
	private final GeneralOutputMaker	main;
	private String						inputPath;
	private String						outputPath;
	private boolean flag;
	private boolean xx = true;
	//boolean pointerFlag

	public CodeGenerator(List<UserType> IR, GeneralOutputMaker main) {
		this.IR = IR;
		this.main = main;
	}

	public void make() {
		// : . . ?
		System.out.println("\n\n");




		// typeDeclarations.print(b);
		// typeDeclarations.close();

		PrintWriter file = null;

		StringBuilder s = new StringBuilder()
				.append("module Types where")
				.append('\n')
				.append("\nimport Data.Int")
				.append("\nimport D_Types")
				.append('\n');

		for (UserType userType : IR) {
			if (userType.getSuperType() == null) {
				declareType(userType, s, new StringBuilder());
			}
		}

		s.append('\n');

		for (UserType userType : IR) {
			for (Field field : userType.getFields()) {
				String methodName = "c'" + userType.getName().capital() + '_' + field.getName().capital();
				String type = getHaskellType(field);

				if (!flag) {
					s.append("\n").append(methodName).append(" (").append(somethingDataConstructor(field)).append(" value) = value");
				}
			}
		}
		//		System.out.println(s);

		//		for (UserType userType : IR) {
		//			try {
		//				String moduleName = "Access" + escape(userType.getName().capital().toString());
		//
		//				String path = "C:/output/" + moduleName + ".hs";
		//				System.out.println(path);
		//
		//				//			PrintWriter file = main.open(path);
		//				//			file = new PrintWriter(new BufferedWriter(new FileWriter("C:/output/")));
		//
		//				s
		//				.append("module ").append(moduleName).append(" where\n")
		//				.append("\nimport Types")
		//				.append("\nimport Deserialize")
		//				.append("\nimport D_Types")
		//				.append("\nimport Methods")
		//				.append("\nimport Data.ByteString.Lazy.Char8 as C")
		//				.append("\nimport Data.List as L")
		//				.append("\nimport Data.Binary.Get")
		//				.append("\nimport Data.Int")
		//				.append("\nimport ReadFields")
		//				.append("\nimport Data.IORef")
		//				.append("\nimport System.IO.Unsafe")
		//				.append("\n")
		//				//				.append("\nfilePath = \"INSERT FILEPATH\"")
		//				.append("\nfilePath = \"C:/input/Sen.sf\"")
		//				.append("\n");
		//
		//
		//				for (Field field : userType.getFields()) {
		//					String n = field.getName().toString();
		//					String nC = field.getName().capital().toString();
		//					String type = getHaskellType(field);
		//					String f0 = "get" + nC + 's';
		//					String f1 = "read" + nC + "sInType";
		//					String f2 = n + "Convert";
		//
		//					//					readAsInType :: [FieldDescTest] -> [String]
		//					//							readAsInType ((name, data', _) : rest)
		//					//							 | name == "a" = L.map aConvert data'
		//					//							 | otherwise = readAsInType rest
		//
		//					s
		//					.append("\n")
		//					.append("\n").append(f0).append(" :: [").append(type).append(']')
		//					.append("\n").append(f0).append(" = go $ ((\\(a,b,c) -> c) . unsafePerformIO) (readState filePath)")
		//					.append("\n where go (TD (id, name, fDs, subTypes) : rest)")
		//					.append("\n        | name == \"").append(userType.getName().toString()).append("\" = ").append(f1).append(" fDs")
		//					.append("\n        | not $ L.null (go subTypes) = go subTypes")
		//					.append("\n        | otherwise = go rest")
		//					.append("\n       go _ = []")
		//					.append("\n")
		//					.append("\n")
		//					.append("\n").append(f1).append(" :: [FieldDescTest] -> [").append(type).append(']')
		//					.append("\n").append(f1).append(" ((name, data', _) : rest)")
		//					.append("\n | name == \"").append(n).append("\" = L.map ").append(f2).append(" data'")
		//					.append("\n | otherwise = ").append(f1).append(" rest")
		//					.append("\n");
		//
		//					//	file.print(s);
		//					//	System.out.println(s);
		//				}
		//			} catch (Exception e) {
		//				System.out.println("EXCEPTION!");
		//				e.printStackTrace();
		//			} finally {
		//
		//				//		file.close();
		//			}
		//		for (UserType t : IR) {
		//			file.println(t.getName().capital());
		//			// file.println(main.getPath());
		//
		//			for (Field f : t.getFields()) {
		//				file.printf(" - %s %s\n", f.getType().toString(), f.getName().camel());
		//				file.print(s.toString());
		//			}
		//		}

		// System.out.println(s);
		//		}
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		c.setContents(new StringSelection(s.toString()), null);
		//		copyStaticFiles();
	}

	private String escape(String string) {
		return string;
	}

	private String declareType(UserType t, StringBuilder s, StringBuilder superFields) {
		String n = t.getName().capital();

		s.append("\ndata I_").append(n).append(" = ").append(n).append('\'').append(n).append(' ').append(n);

		for (UserType subType : getTransitiveSubTypes(t)) {
			s.append(" | ").append(n).append('\'').append(subType.getName().capital()).append(' ').append(subType.getName().capital());
		}

		//Result: data I_A = A'A A | A'BA BA | A'CA CA | A'DB DB | A'ED ED


		s.append("\ntype ").append(t.getName().capital()).append(" = (");

		StringBuilder newFields = new StringBuilder();

		for (Field f : t.getFields()) {
			newFields.append(getHaskellType(f)).append(", ");
		}

		StringBuilder fields = new StringBuilder(superFields).append(newFields);

		s.append(fields.substring(0, fields.length()-2)).append(')');

		for (UserType subType : t.getSubTypes()) {
			newFields.append(declareType(subType, s, fields));
		}
		return newFields.toString();
	}




	private List<UserType> getTransitiveSubTypes(UserType type) {
		//requires a copy, not a reference, otherwise it has side effects
		List<UserType> subTypes = new LinkedList<>();
		subTypes.addAll(type.getSubTypes());

		List<UserType> justTransitiveSubTypes = new LinkedList<>();

		for (UserType subType : subTypes) {
			justTransitiveSubTypes.addAll(getTransitiveSubTypes(subType));
		}
		subTypes.addAll(justTransitiveSubTypes);
		return subTypes;
	}

	private String somethingDataConstructor(Field field) {
		switch (field.getType().toString()) {
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
				return field.getType().toString();
		}
	}

	private String getHaskellType(Field f) {
		flag = false;


		Name n = f.getType().getName();

		String lowercase = n.lower();
		String typeName = n.capital();

		String prefix = "";

		if (f.getType() instanceof ReferenceType && !f.getType().getName().getSkillName().equals("string")) {
			prefix = "Maybe ";
			flag = true;
		}

		//		System.out.println(lowercase);

		int l = lowercase.length();

		if (lowercase.startsWith("list")) {
			return '[' + prefix + baseToHaskell(typeName.substring(5, l - 1)) + ']';
		} else if (lowercase.startsWith("set")) {
			return '[' + prefix + baseToHaskell(typeName.substring(4, l - 1)) + ']';
		} else if (lowercase.startsWith("map")) {
			return "(M.map (" + baseToHaskell(substring(typeName, 4, ',')) + ' '
					+ baseToHaskell(substring(typeName.substring(0, l - 1), ",")) + ")";
		} else if (lowercase.endsWith("[]")) {
			return '[' + prefix + baseToHaskell(typeName.substring(0, l - 2)) + ']';
		} else if (lowercase.endsWith("]")) {
			return '[' + prefix + baseToHaskell(typeName.substring(0, l - 3)) + ']';
		} else {
			return prefix + baseToHaskell(typeName);
		}
	}

	private String baseToHaskell(String string) {
		//		System.out.println("-----\n" + string + "\n-----\n");
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

	public void copyStaticFiles() {
		// set filepaths here
		//		inputPath = System.getProperty("user.dir") + "/src/main/resources/haskell/";
		//		outputPath = System.getProperty("user.dir") + "/tmp/";

		inputPath = "C:/input/";
		outputPath = "C:/output/";
		// I can't make this field global ... ??
		String[] fileNames = {"Deserialize.hs", "Methods.hs", "ReadFields.hs", "Types.hs"};

		try {
			for (String fileName : fileNames) {
				Files.copy(Paths.get(inputPath + fileName), Paths.get(outputPath + fileName));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String substring(String string, String start) {
		if (start.isEmpty()) { return string; }

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != start.charAt(0)) {
				continue;
			} else if (string.substring(i, i + start.length()).equals(start)) { return string.substring(i + start.length()); }
		}
		return null;
	}

	public static String substring(String string, int start, char end) {
		for (int i = start; i < string.length(); i++) {
			if (string.charAt(i) == end) { return string.substring(start, i); }
		}
		return null;
	}



}
