module Interface where

import Deserialize
import Types
import Methods
import Data.ByteString.Lazy.Char8 as C
import Data.List as L
import Data.Binary.Get
import Data.Word
import ReadFields
import Data.IORef
import System.IO.Unsafe

--filePath = "C:\\HaskellTest\\cc.sf"
filePath = "C:\\skill\\src\\test\\resources\\genbinary\\auto\\accept\\date.sf"
nameAges = "date"

getAges :: [Word64]
getAges = go $ ((\(a,b,c) -> b) . unsafePerformIO . readIORef) state
          where go ((string, count, _, _, fieldDescriptors) : rest)
                  | string == nameAges = readAgesInType fieldDescriptors count
                  | otherwise          = go rest


readAgesInType :: [FieldDesc] -> Int -> [Word64]
readAgesInType ((_, Just nameID, Just getter, offset, data') : rest) count
  | strings !! nameID == "date" = L.map ageConvert $ runGet (repeatGet getter count) data'
  | otherwise                   = readAgesInType strings rest count

ageConvert :: Something -> Word64
ageConvert (GV64 value) = value
ageConvert _            = error "Error in Interface: Unexpected Type"

getStrings   = fst $ (unsafePerformIO . readIORef) state
getTypeDescs = snd $ (unsafePerformIO . readIORef) state
initiate     = initialize filePath