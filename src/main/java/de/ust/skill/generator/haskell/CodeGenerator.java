package de.ust.skill.generator.haskell;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.ust.skill.ir.Field;
import de.ust.skill.ir.Name;
import de.ust.skill.ir.ReferenceType;
import de.ust.skill.ir.UserType;

public class CodeGenerator {

    private final List<UserType> IR;
    private final GeneralOutputMaker main;

    private static final String[] fileNames = { "Deserialize.hs", "Methods.hs", "ReadFields.hs", "Serialize.hs",
            "CC.hs", "D_Types.hs" };

    public CodeGenerator(List<UserType> IR, GeneralOutputMaker main) {
        this.IR = IR;
        this.main = main;
    }

    public void make() {

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
        // typeDeclarations.print(b);
        // typeDeclarations.close();

        writeFile_Types();
        writeFile_Follow();

        for (UserType userType : IR) {
            writeFile_Access(userType);
        }

        copyStaticFiles();
    }

    private void writeFile_Types() {
        StringBuilder s = new StringBuilder().append("module Types where").append('\n').append("\nimport Data.Int")
                .append("\nimport D_Types").append('\n');

        for (UserType userType : IR) {
            if (userType.getSuperType() == null) {
                declareType(userType, s);
            }
        }

        s.append('\n');

        for (UserType userType : IR) {
            List<Field> fields = getAllFields(userType);

            for (Field field : fields) {
                String methodName = "c'" + userType.getName().capital() + '_' + field.getName().capital();

                s.append("\n").append(methodName).append(" (").append(somethingDataConstructor(field))
                        .append(" value) = value");
            }
        }
        output(s, "Types.hs");
    }

    private void writeFile_Follow() {
        StringBuilder s = new StringBuilder().append("module Follow where").append("\n").append("\nimport Deserialize")
                .append("\nimport D_Types").append("\nimport Types").append("\nimport Methods").append("\n");

        for (UserType userType : IR) {
            String n = userType.getName().capital();

            s.append("\nfollow").append(n).append(" :: [TypeDesc] -> Ref -> Maybe ").append(n).append("\nfollow")
                    .append(n).append(" _ (_,-1)    = Nothing").append("\nfollow").append(n)
                    .append(" tDs (i1,i2) = Just (i2 `extract").append(n).append("` (i1 `reach` tDs))").append("\n");

            List<Field> fields = getAllFields(userType);

            StringBuilder params = new StringBuilder("[");
            StringBuilder rValue = new StringBuilder("(");

            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);

                params.append("(_, f").append(i).append(", _)");
                rValue.append("(c'").append(n).append('_').append(f.getName().capital()).append("(f").append(i)
                        .append(" !! i))");

                if (i < fields.size() - 1) {
                    params.append(", ");
                    rValue.append(", ");
                }
            }
            params.append("]");
            rValue.append(")");

            s.append("\nextract").append(n).append(" :: Int -> [FieldDescTest] -> ").append(n).append("\nextract")
                    .append(n).append(" i ").append(params).append(" = ").append(rValue).append("\n");
        }
        /*
         * WRITE S IN FILE ("Follow.hs") HERE
         */

        output(s, "Follow.hs");
    }

    private void writeFile_Access(UserType userType) {
        String uTN = userType.getName().capital();
        String uTL = uTN.toLowerCase();
        String f0 = "get" + uTN + 's';
        String f1 = "read" + uTN + 's';
        String moduleName = "Access" + escape(uTN);

        StringBuilder s = new StringBuilder().append("module ").append(moduleName).append(" where\n")
                .append("\nimport Types").append("\nimport Deserialize").append("\nimport D_Types")
                .append("\nimport Methods").append("\nimport Follow").append("\nimport Data.Int")
                .append("\nimport Data.IORef").append("\nimport System.IO.Unsafe").append("\n")
                // .append("\nfilePath = \"INSERT FILEPATH\"")
                .append("\nfilePath = \"C:/input/Sen.sf\"").append("\n").append("\n").append(f0).append("'' :: [")
                .append(uTN).append(']').append("\n").append(f0).append("'' = ").append(f0)
                .append(" $ (unsafePerformIO . createState) filePath").append("\n").append("\n").append(f0)
                .append("' :: [").append(uTN).append(']').append("\n").append(f0).append("' = ").append(f0)
                .append(" readLatestState").append("\n").append("\n").append(f0).append(" :: State -> [").append(uTN)
                .append(']').append("\n").append(f0).append(" = ").append(f1).append(" . readTDs").append("\n where ")
                .append(f1).append(" [] = []").append("\n       ").append(f1)
                .append(" (TD (id, name, fDs, subTypes) : rest)").append("\n         | name == \"").append(uTL)
                .append("\" && (not . null) fDs = go (0, (length . snd' . head) fDs) fDs")
                .append("\n         | not $ null (").append(f1).append(" subTypes) = ").append(f1).append(" subTypes")
                .append("\n         | otherwise = ").append(f1).append(" rest")
                .append("\n            where go :: (Int, Int) -> [FieldDescTest] -> [").append(uTN).append("]")
                .append("\n                  go _ [] = []").append("\n                  go (id, size) fDs")
                .append("\n                      | id == size = []")
                .append("\n                      | otherwise  = (id `extract").append(uTN)
                .append("` fDs) : go (id + 1, size) fDs").append("\n");

        List<Field> fields = getAllFields(userType);

        StringBuilder params = new StringBuilder("(");

        for (int i = 0; i < fields.size(); i++) {
            params.append('f').append(i);

            if (i < fields.size() - 1) {
                params.append(", ");
            }
        }

        params.append(')');

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);

            String fN = field.getName().capital();
            String fT = getHaskellType(field);

            String f2 = "get" + uTN + '_' + fN;
            String f3 = f2 + 's';

            s.append("\n").append(f2).append(" :: ").append(uTN).append(" -> ").append(fT).append("\n").append(f2)
                    .append(' ').append(params).append(" = f").append(i).append("\n").append("\n").append(f3)
                    .append(" :: State -> [").append(fT).append(']').append("\n").append(f3).append(" state = map ")
                    .append(f2).append(" (").append(f0).append(" state)");

            if (i < fields.size() - 1) {
                s.append('\n');
            }
        }
        output(s, "Access" + uTN + ".hs");
    }

    private void output(StringBuilder s, String path) {
        writeInFile(s, path);
        appendToClipboard(s);
    }

    private void writeInFile(StringBuilder s, String path) {
        PrintWriter out = main.open(main.packagePath() + "/" + path);

        out.write(s.toString());

        out.close();

    }

    private void appendToClipboard(StringBuilder s) {
        try {
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringBuilder p = new StringBuilder((String) c.getData(DataFlavor.stringFlavor));
            c.setContents(new StringSelection(p.append(s).toString()), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escape(String string) {
        return string;
    }

    private void declareType(UserType t, StringBuilder s) {
        String n = t.getName().capital();

        if (!t.getSubTypes().isEmpty()) {
            s.append("\ndata ").append(n).append("' = ").append(n).append('\'').append(n).append(' ').append(n);
        }

        for (UserType subType : getTransitiveSubTypes(t)) {
            s.append(" | ").append(n).append('\'').append(subType.getName().capital()).append(' ')
                    .append(subType.getName().capital());
        }

        // Result: data I_A = A'A A | A'BA BA | A'CA CA | A'DB DB | A'ED ED

        s.append("\ntype ").append(n).append("  = (");

        StringBuilder fields = new StringBuilder();

        for (Field f : getAllFields(t)) {
            fields.append(getHaskellType(f)).append(", ");
        }

        if (fields.length() > 0) {
            s.append(fields.substring(0, fields.length() - 2));
        } else {
            // s.delete(s.length()-1, s.length());
        }

        s.append(')');

        for (UserType subType : t.getSubTypes()) {
            declareType(subType, s);
        }
        return;
    }

    private List<Field> getAllFields(UserType t) {
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

    private List<UserType> getTransitiveSubTypes(UserType type) {
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
            // return field.getType().toString();
            return "GRef";
        }
    }

    private String getHaskellType(Field f) {
        if (isReference(f)) {
            return "Ref";
        }

        Name n = f.getType().getName();

        String lowercase = n.lower();
        String typeName = n.capital();

        // System.out.println(lowercase);

        int l = lowercase.length();

        if (lowercase.startsWith("list")) {
            return '[' + baseToHaskell(typeName.substring(5, l - 1)) + ']';
        } else if (lowercase.startsWith("set")) {
            return '[' + baseToHaskell(typeName.substring(4, l - 1)) + ']';
        } else if (lowercase.startsWith("map")) {
            return "(M.map (" + baseToHaskell(Methods.substring(typeName, 4, ',')) + ' '
                    + baseToHaskell(Methods.substring(typeName.substring(0, l - 1), ",")) + ")";
        } else if (lowercase.endsWith("[]")) {
            return '[' + baseToHaskell(typeName.substring(0, l - 2)) + ']';
        } else if (lowercase.endsWith("]")) {
            return '[' + baseToHaskell(typeName.substring(0, l - 3)) + ']';
        } else {
            return baseToHaskell(typeName);
        }
    }

    private boolean isReference(Field f) {
        return f.getType() instanceof ReferenceType && !f.getType().getName().getSkillName().equals("string");
    }

    private String baseToHaskell(String string) {
        // System.out.println("-----\n" + string + "\n-----\n");
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
        final String inputPath = "deps/haskell/";
        final String outputPath = main.outPath() + "/generated/" + main.packagePath() + "/";

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