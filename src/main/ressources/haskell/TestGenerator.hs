module TestGenerator where

import Test.HUnit
import System.Directory
import Data.List

------------------------------------------------------------------------------------------------------------------
-- procedure to generate test procedures (.hs files). The method call is >> generate sfPath outputPath << where --
-- sfPath      = a directory path that holds any number of sf files                                             --
-- outputPath: = the directory where the test  are copied to                                                    --
-- Both arguments must be given as strings with a slash at the end. A valid call could be                       --
--           >> generate "C:/skill/src/test/resources/genbinary/auto/accept/" "C:/output/" <<                   --
-- Tests will only be generated for files ending on ".sf"                                                       --
------------------------------------------------------------------------------------------------------------------

generate sfPath outputPath = do fileNames <- getDirectoryContents sfPath
                                let fileNames' = filter (\a -> (take 3 . reverse) a == "fs.") fileNames
                                foldr ((>>) . createTestFile sfPath outputPath) (return ()) fileNames'
                                createHeadProcedure sfPath outputPath fileNames'

createTestFile :: FilePath -> FilePath -> String -> IO ()
createTestFile sfPath outputPath sfName
                 | length sfName < 5 = return ()
                 | otherwise = writeFile (outputPath ++ "ZTest_" ++ cut sfName ++ ".hs") $
                      "module ZTest_" ++ (init . init . init) sfName ++" where"
                 ++   "\nimport Test.HUnit" ++ "\nimport Deserialize" ++ "\nimport Data.IORef" ++ "\nimport Methods"
                 ++   "\n\nrun = TestCase $ printTestName \"" ++ cut sfName ++ "\""
                 ++   " >> initialize \"" ++ sfPath ++ sfName ++ "\""

createHeadProcedure :: FilePath -> FilePath -> [String] -> IO ()
createHeadProcedure sfPath outputPath names = writeFile (outputPath ++ "RunTests.hs") $
                    "import Test.HUnit\n"
                  ++ concatMap (\name -> "import ZTest_" ++ cut name ++ "\n") names
                  ++ "\nmain = runTestTT $ TestList ["
                  ++ intercalate ",\n" (map createLabel names) ++ "  ]"
                         where createLabel name = "  TestLabel \"" ++ cut name ++ "\" ZTest_" ++ cut name ++ ".run"

cut = init . init . init