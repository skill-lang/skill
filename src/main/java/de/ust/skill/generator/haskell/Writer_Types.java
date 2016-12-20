package de.ust.skill.generator.haskell;

import java.util.List;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.UserType;
import de.ust.skill.generator.haskell.CodeGenerator;

public class Writer_Types {
	private final static byte LIST = 0;
	private final static byte SET = 1;
	private final static byte FARRAY = 2;
	private final static byte VARRAY = 3;
	private final static byte MAP = 4;


	public static void go (List<UserType> IR) {
		StringBuilder s = new StringBuilder()
				.append("module Types where")
				.append('\n')
				.append("\nimport qualified Data.Map as M")
				.append("\nimport Data.Int")
				.append("\nimport Memory")
				.append("\nimport Methods")
				.append('\n');

		for (UserType userType : IR) {
			if (userType.getSuperType() == null) {
				declareType(userType, s);
			}
		}

		s.append('\n');

		for (UserType userType : IR) {
			String tN = userType.getName().capital();
			List<Field> fields = CodeGenerator.getAllFields(userType);

			for (Field field : fields) {
				String c_name = "c'" + tN + '_' + field.getName().capital();
				String d_name = "d'" + tN + '_' + field.getName().capital();
				String fTN = field.getType().toString();
				String fN = field.getName().capital();

				if (fTN.startsWith("list")) {
					writeSomethingConverters(s, tN, fTN, fN, LIST);
				} else if (fTN.startsWith("set")) {
					writeSomethingConverters(s, tN, fTN, fN, SET);
				} else if (fTN.startsWith("map")) {
					writeSomethingConverters(s, tN, fTN, fN, MAP);
				} else if (fTN.endsWith("[]")) {
					writeSomethingConverters(s, tN, fTN, fN, VARRAY);
				} else if (fTN.endsWith("]")) {
					writeSomethingConverters(s, tN, fTN, fN, FARRAY);
				} else {
					String dataConstructor = CodeGenerator.somethingDataConstructor(userType, field);
					
					s.append("\n").append(c_name).append(" value = ").append(dataConstructor).append(" value")
					 .append("\n").append(d_name).append(" (").append(dataConstructor).append(" value) = value");
				}
				s.append("\n");
			}
		}
		CodeGenerator.writeInFile(s, "Types.hs");
	}

	private static void writeSomethingConverters(StringBuilder s, String tN, String fTN, String fN, byte kind) {
		String constructor = tN + '_' + fN;
		
		StringBuilder c = new StringBuilder();
		StringBuilder d = new StringBuilder();
		
		c.append("\nc'").append(constructor).append(" values = ");
		d.append("\nd'").append(constructor).append(" (");
		
		if (kind == MAP) {
			String fTN1 = Methods.substring(fTN, "<", ",");
			String fTN2 = Methods.substring(fTN, ",", ">");
			
			String c1 = CodeGenerator.somethingDataConstructor(fTN1);
			String c2 = CodeGenerator.somethingDataConstructor(fTN2);
			
			s
			.append(c).append("GMap $ Prelude.map (\\(v1,v2) -> (").append(c1).append(" v1, ").append(c2).append(" v2)) (M.assocs values)")
			.append(d).append("GMap values) = M.fromList $ Prelude.map (\\(").append(c1).append(" v1, ").append(c2).append(" v2) -> (v1,v2)) values");
		} else {
			if (kind == VARRAY) {
				fTN = fTN.substring(0, fTN.length() - 2);
				c.append("GVArray");
				d.append("GVArray");
			} else if (kind == FARRAY) {
				fTN = fTN.substring(0, fTN.length() - 3);
				c.append("GFArray");
				d.append("GFArray");
			} else if (kind == SET) {
				fTN = fTN.substring(4, fTN.length() - 1);
				c.append("GSet");
				d.append("GSet");
			} else if (kind == LIST) {
				fTN = fTN.substring(5, fTN.length() - 1);
				c.append("GList");
				d.append("GList");
			}
			
			String primitivum = CodeGenerator.somethingDataConstructor(fTN);
			c.append(" $ Prelude.map ").append(primitivum).append(" values");
			d.append(" values) = Prelude.map ").append("(\\(").append(primitivum).append(" v) -> v) values");
			
			s.append(c).append(d);
		}
	}

	private static void declareType(UserType t, StringBuilder s) {
		String n = t.getName().capital();

		if (!t.getSubTypes().isEmpty()) {
			s.append("\ndata ").append(n).append("' = ").append(n).append('\'').append(n).append(' ').append(n);


			for (UserType subType : CodeGenerator.getTransitiveSubTypes(t)) {
				s.append(" | ").append(n).append('\'').append(subType.getName().capital()).append(' ')
				.append(subType.getName().capital());
			}

			s.append(" deriving (Show, Eq)");
		}
		// Result: data I_A = A'A A | A'BA BA | A'CA CA | A'DB DB | A'ED ED

		s.append("\ntype ").append(n).append("  = (");

		StringBuilder fields = new StringBuilder();

		for (Field f : CodeGenerator.getAllFields(t)) {
			fields.append(CodeGenerator.getHaskellType(f, CodeGenerator.LEAVE_REFERENCES)).append(", ");
		}

		if (fields.length() > 0) {
			s.append(fields.substring(0, fields.length() - 2));
		}

		s.append(')');

		for (UserType subType : t.getSubTypes()) {
			declareType(subType, s);
		}
	}
}