module ZTest_trivialType where
import Test.HUnit
import Deserialize
import Data.IORef

run = TestCase $
 do initialize "C:\\skill\\src\\test\\resources\\genbinary\\auto\\accept\\trivialType.sf"
    (strings, typeDescs) <- readIORef ordered
    let (nameID, count, superID, lBPO, fieldDescs) = head typeDescs
    assertEqual "see if strings are empty" False (null strings)
    assertEqual "see if type descriptors are empty" False (null typeDescs)
    assertEqual "see if field descriptors are empty" False (null fieldDescs)