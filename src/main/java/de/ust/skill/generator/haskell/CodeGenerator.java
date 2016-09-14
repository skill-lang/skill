package de.ust.skill.generator.haskell;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.UserType;
import static java.nio.file.StandardCopyOption.*;

public class CodeGenerator {

	private final List<UserType>		IR;
	private final GeneralOutputMaker	main;
	private String						inputPath;
	private String						outputPath;

	public CodeGenerator(List<UserType> IR, GeneralOutputMaker main) {
		this.IR = IR;
		this.main = main;
	}

	public void make() {
		// : . . ?
		System.out.println("\n\n");
		
		
		String binaryPath = "C:\\\\skill\\\\src\\\\test\\\\resources\\\\genbinary\\\\auto\\\\accept\\\\insertnamehere.sf";

		PrintWriter typeDeclarations = main.open("SkillTypes.hs");

		StringBuilder b = new StringBuilder();

		for (UserType userType : IR) {
			declareType(userType, b);
		}
		//		typeDeclarations.print(b);
		//		typeDeclarations.close();

		System.out.print(b);

		//		for (UserType userType : IR) {
		//			String skillName = userType.getName().capital().toString();
		//
		//			PrintWriter file = main.open(skillName + "Access.hs");
		//
		//			StringBuilder s = new StringBuilder()
		//					.append("module ").append(skillName).append("Access where\n")
		//					.append("\nimport Deserialize")
		//					.append("\nimport Types")
		//					.append("\nimport Methods")
		//					.append("\nimport Data.ByteString.Lazy.Char8 as C")
		//					.append("\nimport Data.List as L")
		//					.append("\nimport Data.Binary.Get")
		//					.append("\nimport Data.Int")
		//					.append("\nimport ReadFields")
		//					.append("\nimport Data.IORef")
		//					.append("\nimport System.IO.Unsafe")
		//					.append("\n")
		//					.append("\nfilePath = \"").append(binaryPath).append("\"")
		//					.append("\ninitiate = initialize filePath")
		//					.append("\n");
		//
		//			
		//			for (Field field : userType.getFields()) {
		//				String n = field.getName().toString();
		//				String nC = field.getName().capital().toString();
		//				String type = toHaskell(field.getType().toString());
		//				String f0 = "get" + nC + 's';
		//				String f1 = "read" + nC + "sInType";
		//				String f2 = n + "Convert";
		//
		//				s
		//				.append("\n")
		//				.append("\n").append(f0).append(" :: [").append(type).append(']')
		//				.append("\n").append(f0).append(" = go $ ((\\(a,b,c) -> b) . unsafePerformIO . readIORef) ordered")
		//				.append("\n   where go ((name, count, _, _, fieldDescriptors) : rest)")
		//				.append("\n          | name == \"").append(userType.getName().toString()).append("\" = ").append(f1).append(" fieldDescriptors count")
		//				.append("\n          | otherwise = go rest")
		//				.append("\n         go _ = error \"Did not find field in the .sf file\"")
		//				.append("\n")
		//				.append("\n")
		//				.append("\n").append(f1).append(" :: [FieldDesc] -> Int -> [").append(type).append(']')
		//				.append("\n").append(f1).append(" ((_, Just name, Just getter, data') : rest) count")
		//				.append("\n           | name == \"").append(n).append("\" = L.map ").append(f2).append(" $ runGet (repeatGet getter count) data'")
		//				.append("\n           | otherwise = ").append(f1).append(" rest count")
		//				.append("\n")
		//				.append("\n").append(f2).append(" :: Something -> ").append(type)
		//				.append("\n").append(f2).append(" (").append(somethingDataConstructor(field)).append(" value) = value")
		//				.append("\n").append(f2).append(" _ = error \"Error in Interface: Unexpected Type\"")
		//				.append("\n");
		//			}

		//			file.print(s);
		//			file.close();
		//	}
		// for (UserType t : IR) {
		// file.println(t.getName().capital());
		// // file.println(main.getPath());
		//
		// for (Field f : t.getFields()) {
		//		 file.printf(" - %s %s\n", f.getType().toString(), f.getName().camel());
		//		file.print(s.toString());
		// }
		// }
		copyStaticFiles();

		//		System.out.println(s);
	}

	private void declareType(UserType t, StringBuilder s) {
		s.append("data ").append(t.getSkillName()).append(" = C'").append(t.getSkillName()).append(" (");

		for (Field f : t.getFields()) {
			if (true) { //if it's a pointer @TODO
				s.append("Maybe ");
			}
			s.append(toHaskell(f.getType().toString())).append(", ");
		}

		s.delete(s.length()-2, s.length()).append(")");


	}




	private String somethingDataConstructor(Field field) {
		switch (field.getType().toString()) {
			case "bool": return "GBool";
			case "string": return "GString";
			case "i8": return "GWord8";
			case "i16": return "GWord16";
			case "i32": return "GWord32";
			case "i64": return "GWord64";
			case "v64": return "GV64";
			case "f32": return "GFloat";
			case "f64": return "GDouble";
			default: return "qwerty-Error";
		}
	}

	private String toHaskell(String typeName) {
		System.out.println(typeName);

		int l = typeName.length();

		if (typeName.startsWith("list")) {
			return '[' + baseToHaskell(typeName.substring(5, l-1)) + ']';
		} else if (typeName.startsWith("set")) {
			return '[' + baseToHaskell(typeName.substring(4, l-1)) + ']';
		} else if (typeName.startsWith("map")) {
			return "(M.map " + baseToHaskell(substring(typeName, 4, ',')) + ' ' + baseToHaskell(substring(typeName.substring(0, l-1), ",")) + ")";
		} else if (typeName.endsWith("[]")) {
			return '[' + baseToHaskell(typeName.substring(0, l-2)) + ']';
		} else if (typeName.endsWith("]")) {
			return '[' + baseToHaskell(typeName.substring(0, l-3)) + ']';
		} else {
			return baseToHaskell(typeName);
		}
	}

	private String baseToHaskell(String string) {
		System.out.println("-----\n" + string + "\n-----\n");
		switch (string) {
			case "bool": return "Bool";
			case "string": return "Int";
			case "i8": return "Int8";
			case "i16": return "Int16";
			case "i32": return "Int32";
			case "i64": return "Int64";
			case "v64": return "Int64";
			case "f32": return "Float";
			case "f64": return "Double";
			case "annotation" : return "Pointer";
			default: return "qwerty-Error";
		}
	}

	public void copyStaticFiles() {
		// set filepaths here
		inputPath = "C:\\Workspace\\Hask2\\src\\";
		outputPath = "C:\\output\\haskell\\generated\\";
		// I can't make this field global ... ??
		String[] fileNames = {"Deserialize.hs", "Methods.hs", "ReadFields.hs", "Types.hs"};

		try {
			for (String fileName : fileNames) {
				Files.copy(Paths.get(inputPath + fileName), Paths.get(outputPath + fileName));
			}

			// Files.copy("files\\Methods", filePath + "Methods");
			// Files.copy("files\\ReadFields", filePath + "ReadFields");
			// Files.copy("files\\Types", filePath + "Types.hs");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String substring(String string, String start) {
		if (start.isEmpty()) {
			return string;
		}

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != start.charAt(0)) {
				continue;
			} else if (string.substring(i, i + start.length()).equals(start)) {
				return string.substring(i + start.length());
			}
		}
		return null;
	}
	
	public static String substring(String string, int start, char end) {
		for (int i = start; i < string.length(); i++) {
			if (string.charAt(i) == end) {
				return string.substring(start, i);
			}
		}
		return null;
	}
	
	
	
}
