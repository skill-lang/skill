package de.ust.skill.generator.c;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;


public class GeneratorTest {

    private final static File sourceRootFolder = new File ( "src/test/resources/c/" );
    private final static File targetRootFolder = new File ( "testsuites/cTestSuite/src/generated" );

    @BeforeClass
    public static void prepare () {
        targetRootFolder.mkdirs ();
        deleteFolderContents ( targetRootFolder );
    }

    private void generateBinding ( String prefix, String sourceFile, String targetDir ) throws Exception {
        Main.main ( new String[]{"-p", prefix, sourceRootFolder.getAbsolutePath () + "/" + sourceFile, targetRootFolder.getAbsolutePath () + "/" + targetDir} );
    }

    @Test
    public void doGenerateBindings () throws Exception {
        generateBinding ( "", "date.skill", "date" );
        generateBinding ( "", "basic_types.skill", "basic_types" );
        generateBinding ( "", "empty.skill", "empty" );
        generateBinding ( "", "const.skill", "const" );
        generateBinding ( "", "auto.skill", "auto" );
        generateBinding ( "", "float.skill", "float" );
        generateBinding ( "", "node.skill", "node" );
        generateBinding ( "", "subtypesExample.skill", "subtypes" );
        generateBinding ( "", "container1.skill", "container1" );
        generateBinding ( "", "container_user_type.skill", "container_user_type" );
        generateBinding ( "", "container_annotation.skill", "container_annotation" );
        generateBinding ( "", "container_string.skill", "container_string" );
        generateBinding ( "", "annotation.skill", "annotation" );
        
        // Generate two bindings with prefix into the same directory, so that the test can compile two bindings together
        generateBinding ( "sub", "subtypesExample.skill", "subtypes_prefix" );
        generateBinding ( "date", "date.skill", "date_prefix" );
        
    }

    private static void deleteFolderContents ( File folder ) {
        File[] files = folder.listFiles ();
        if ( files != null ) {
            for ( File currentFile: files ) {
                if ( currentFile.isDirectory () ) {
                    deleteFolderContents ( currentFile );
                } else {
                    currentFile.delete ();
                }
            }
        }
        folder.delete ();
    }
}
