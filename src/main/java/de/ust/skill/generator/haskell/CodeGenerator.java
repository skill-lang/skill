package de.ust.skill.generator.haskell;

import java.io.PrintWriter;
import java.util.List;

import de.ust.skill.ir.Field;
import de.ust.skill.ir.UserType;

public class CodeGenerator {

    private final List<UserType> IR;
    private final GeneralOutputMaker main;

    public CodeGenerator(List<UserType> IR, GeneralOutputMaker main) {
        this.IR = IR;
        this.main = main;
    }

    public void make() {
        for (UserType t : IR) {
            PrintWriter file = main.open(t.getName().capital() + ".hs");
            file.println(t.getName().capital());
            
            for(Field f : t.getFields()){
                file.printf(" - %s %s\n", f.getType().toString(), f.getName().camel());
            }
            file.close();
        }
    }
}
