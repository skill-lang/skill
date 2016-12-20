package de.ust.skill.generator.haskell;

import java.util.List;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.UserType;
import de.ust.skill.generator.haskell.CodeGenerator;

public class Writer_Follow {
	public static void go(List<UserType> IR) {
		StringBuilder s = new StringBuilder()
				.append("module Follow where")
				.append("\n")
				.append("\nimport qualified Data.Map as M")
				.append("\nimport qualified Data.List as L")
				.append("\nimport Deserialize")
				.append("\nimport Memory")
				.append("\nimport Types")
				.append("\nimport Methods")
				.append("\n");

		for (UserType userType : IR) {

			boolean simple = userType.getSubTypes().isEmpty();
			String s1 = simple ? "" : "'";


			String n = userType.getName().capital();
			
			s
			.append("\nfollow").append(n).append(" :: [TD] -> Ref -> Maybe ").append(n).append(s1)
			.append("\nfollow").append(n).append(" _ (_,-1)    = Nothing")
			.append("\nfollow").append(n).append(" tDs ref");


			List<UserType> concreteTypes = CodeGenerator.getTransitiveSubTypes(userType);
			concreteTypes.add(0, userType);

			for (UserType t : concreteTypes) {
				String c = t.getName().capital();
				String tC = simple ? "" : "$ " + n + '\'' + c + " ";
				String condition = "name == \"" + c.toLowerCase() + '"';
				//		              | name == "C" = A'C extractC tD
				s.append("\n  | ").append(condition).append(" = Just ").append(tC).append("(extract").append(c).append(" pos fDs)");
			}

			s.append("\n  | otherwise = error \"can't identify target of reference\"")
			.append("\n      where (fDs, name, pos) = ref `reach` tDs")
			.append("\n");



			List<Field> fields = CodeGenerator.getAllFields(userType);

			StringBuilder ex_params = new StringBuilder("[");
			StringBuilder ov_params_fDs = new StringBuilder("[");
			StringBuilder ov_params_obj = new StringBuilder("(");
			StringBuilder ex_return = new StringBuilder("(");
			StringBuilder ov_return = new StringBuilder("[");
			StringBuilder ex_where_f = new StringBuilder("[");
			StringBuilder ov_where_op = new StringBuilder("[");
			StringBuilder ex_where_names = new StringBuilder("[");
			StringBuilder ov_where_names = new StringBuilder("[");
			StringBuilder ex_where_indexes = new StringBuilder();
			StringBuilder ov_where_indexes = new StringBuilder();

			String ex_whitespace = "                       ";
			String ov_whitespace = "                     ";

			for (int i = 0; i < fields.size(); i++) {
				Field f = fields.get(i);
				String fN = f.getName().capital();
				String fS = f.getSkillName();;

				//				String sC = CodeGenerator.somethingDataConstructor(userType, f);

				ex_params.append("(n").append(i).append(", f").append(i).append(", _)");
				ov_params_fDs.append("(n").append(i).append(", d").append(i).append(", r").append(i).append(')');
				ov_params_obj.append('o').append(i);
				ex_return.append("(d'").append(n).append('_').append(fN).append("(f !! i").append(i).append("))");
				ov_return.append("(n" + i + ", (op !! i" + i + ") d" + i + ", r" + i + ")");
				ex_where_f.append("f").append(i);
				ov_where_op.append("(c'").append(n).append("_").append(fN).append(" o").append(i).append(")");
				ex_where_names.append("n" + i);
				ov_where_names.append('"' + fS + '"');
				ex_where_indexes.append(ex_whitespace + "i" + i + " = assure $ L.findIndex (== \"" + fS + "\") names");
				ov_where_indexes.append(ov_whitespace + "i" + i + " = assure $ L.findIndex (== n" + i + ") names");




				if (i < fields.size() - 1) {
					ex_params.append(", ");
					ov_params_fDs.append(", ");
					ov_params_obj.append(", ");
					ex_return.append(", ");
					ov_return.append(", ");
					ex_where_f.append(", ");
					ov_where_op.append(", ");
					ex_where_names.append(", ");
					ov_where_names.append(", ");
					ex_where_indexes.append("\n");
					ov_where_indexes.append("\n");
				}


			}
			ex_params.append("]");
			ov_params_fDs.append("]");
			ov_params_obj.append(")");
			ex_return.append(")");
			ov_return.append("]");
			ex_where_f.append("]");
			ov_where_op.append("]");
			ex_where_names.append("]");
			ov_where_names.append("]");

			String f = "override" + n;

			s.append("\n").append("extract" + n).append(" :: Int -> [FD] -> ").append(n)
			.append("\n").append("extract" + n).append(" i ").append(ex_params)
			.append("\n").append("           = ").append(ex_return)
			.append("\n").append("                 where f = map (!! i) ").append(ex_where_f)
			.append("\n").append("                       names = ").append(ex_where_names)
			.append("\n").append(ex_where_indexes)
			.append("\n")
			.append("\n").append(f).append(" :: TD -> Int -> ").append(n).append(" -> TD")
			.append("\n").append(f).append(" (TD e1 e2 e3 fDs e5 e6) index obj = TD e1 e2 e3 (go fDs index obj) e5 e6")
			.append("\n  where go ").append(ov_params_fDs).append(" i ").append(ov_params_obj)
			.append("\n            = ").append(ov_return)
			.append("\n").append("               where op = map (r i) ").append(ov_where_op)
			.append("\n").append("                     names = ").append(ov_where_names)
			.append("\n").append(ov_where_indexes)
			.append("\n");
		}
		CodeGenerator.writeInFile(s, "Follow.hs");
	}
}