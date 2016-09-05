module TestGenerator where

import Test.HUnit
import Deserialize
import System.IO.Unsafe
import System.Directory
import Data.List

sfDir = "C:\\\\skill\\\\src\\\\test\\\\resources\\\\genbinary\\\\auto\\\\accept\\\\"
outputDirectory = "C:\\workspace\\Hask2\\src\\"



run = do fileNames <- getDirectoryContents sfDir
         let fileNames' = filter (\a -> length a > 5) fileNames
         foldr ((>>) . createTestFile) (return ()) fileNames'
         createHeadProcedure fileNames'



createTestFile :: String -> IO ()
createTestFile sfName
                 | length sfName < 5 = return ()
                 | otherwise = writeFile (buildPath sfName) $
                      "module ZTest_" ++ (init . init . init) sfName ++" where"
                 ++   "\nimport Test.HUnit"
                 ++   "\nimport Deserialize"
                 ++   "\nimport Data.IORef"
                 ++   "\n\nrun = TestCase $"
                 ++   "\n do initialize \"" ++ sfDir ++ sfName ++ "\""
                 ++   "\n    (strings, typeDescs) <- readIORef ordered"
                 ++   "\n    let (nameID, count, superID, lBPO, fieldDescs) = head typeDescs"
                 ++   "\n    assertEqual \"see if strings are empty\" False (null strings)"
                 ++   "\n    assertEqual \"see if type descriptors are empty\" False (null typeDescs)"
                 ++   "\n    assertEqual \"see if field descriptors are empty\" False (null fieldDescs)"

buildPath :: FilePath -> FilePath
buildPath fileName = outputDirectory ++ "ZTest_" ++ (init . init) fileName ++ "hs"


createHeadProcedure :: [String] -> IO ()
createHeadProcedure filePaths = writeFile (outputDirectory ++ "RunTests.hs") $
             "import Test.HUnit\n"
          ++ createImportStatements filePaths
          ++ "\nmain = runTestTT $ TestList ["
          ++ intercalate ",\n" (map writeLabelStatement filePaths) ++ "  ]"

createImportStatements :: [String] -> String
createImportStatements [] = ""
createImportStatements (name : rest) = "import ZTest_" ++ (init . init . init) name ++ "\n" ++ createImportStatements rest


writeLabelStatement :: String -> String
writeLabelStatement name = "  TestLabel \"" ++ name' ++ "\" ZTest_" ++ name' ++ ".run"
                    where name' = (init . init . init) name

