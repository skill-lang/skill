package de.ust.skill.generator.haskell;

import java.util.List;
import de.ust.skill.ir.UserType;
import de.ust.skill.generator.haskell.CodeGenerator;

public class Writer_Interface {
	public static void go(List<UserType> IR) {
		StringBuilder output = new StringBuilder();
		
		output
		.append("\n-----------------------------------------------")
		.append("\n--Access the Binding by writing a method here--")
		.append("\n-------Or by opening this Module in GHCI-------")
		.append("\n-----------------------------------------------")
		.append("\n")
		.append("\nmodule Interface where")
		.append("\n")
		.append("\nimport Controls")
		.append("\nimport Memory")
		.append("\nimport Deserialize")
		.append("\nimport Serialize")
		.append("\nimport ImpossibleImports")
		.append("\nimport Methods")
		.append("\nimport Follow")
		.append("\nimport Types")
		.append("\nimport ReadFields")
		.append("\nimport WriteFields")
		.append("\nimport Test.HUnit");
		
		
		for (UserType userType : IR) {
			output.append("\nimport Access").append(userType.getName().capital()); 
		}
		
		output
		.append("\n")
		.append("\nmain :: IO ()")
		.append("\nmain = putStr \"Write Procedure Here\"");
		
		CodeGenerator.writeInFile(output, "Interface.hs");
	}
}