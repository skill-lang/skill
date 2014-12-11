//package de.ust.skill.generator.c;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import de.ust.skill.ir.UserType;
//import de.ust.skill.parser.Parser;
//
///**
// * This is the entry point of the SKilL Generator for the C programming
// * language. This turns a SKilL specification file into an interface for C to
// * read, create, edit, and write skill files with the instances of the
// * respective definitions.
// *
// * @author Fabian Harth, Timm Felden
// */
//public class OLDMain {
//
//    /**
//     * Help text to print out on the console, if the input parameters were not
//     * correct.
//     */
//    private static void printHelp() {
//        System.out.print("\n" + "usage:\n" + " [options] skillPath outPath \n" + "Opitions:\n"
//                + "-p prefix           set a prefix for emitted code.\n"
//                + "                    This is used for identifier names\n"
//                + "                    in the generated code\n"
//                + "--unsafe            If this option is set, the generated binding\n"
//                + "                    will not execute any type checks when modifying instances.\n"
//                + "                    This improves performance.\n");
//    }
//
//    /**
//     * Main method for the generator.
//     * 
//     * @param args
//     *            command line arguments. Must contain at least skillPath and
//     *            outPath
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        // processing command line arguments
//        if (args.length < 2) {
//            // for the input to be correct, at least skillPath and outPath have
//            // to be specified.
//            printHelp();
//        } else {
//
//            String prefix = null;
//            boolean safe = true;
//            for (int i = 0; i < args.length; i++) {
//                if (args[i].equals("-p")) {
//                    prefix = (args[i + 1]);
//                } else if (args[i].equals("--unsafe")) {
//                    safe = false;
//                }
//            }
//
//            if (prefix == null) {
//                prefix = "";
//            }
//            String skillPath = args[args.length - 2];
//            String outPath = args[args.length - 1];
//
//            File targetfile = new File(skillPath);
//
//            // parse argument code
//            List<UserType> declarations = new ArrayList<>(Parser.process(targetfile, true, true, false)
//                    .removeSpecialDeclarations().getUsertypes());
//
//            OLDGenerator generator = new OLDGenerator(outPath, prefix, declarations, safe);
//
//            // create output using maker chain
//            generator.make();
//        }
//
//    }
// }
