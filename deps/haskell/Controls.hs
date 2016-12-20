module Controls where

import Data.Binary.Get
import Data.IORef
import System.IO.Unsafe
import Memory
import ImpossibleImports
import Methods
import Deserialize
import Serialize

getState :: Int -> State
getState index = ((!! index) . unsafePerformIO) $ readIORef states

readState :: Int -> IO State
readState stateIndex = (!! stateIndex) `fmap` readIORef states

readState' :: IO State
readState' = head `fmap` readIORef states

getState'' :: FilePath -> State
getState'' filePath = unsafePerformIO $ deserialize_ filePath

readState'' :: FilePath -> IO State
readState'' = deserialize_

writeState :: Int -> State -> IO ()
writeState index newState = modifyIORef' states (replace' index newState)

writeState' :: State -> IO ()
writeState' = writeState 0

writeState'' :: FilePath -> IO ()
writeState'' filePath = deserialize filePath >> readState' >>= writeState'

visualize :: Int -> IO ()
visualize index = (getTypeDescriptors . (!! index)) `fmap` readIORef states >>= printTypeDescs

visualize' :: IO ()
visualize' = getTypeDescriptors `fmap` readState' >>= printTypeDescs

visualize'' :: FilePath -> IO ()
visualize'' filePath = getTypeDescriptors `fmap` readState'' filePath >>= printTypeDescs

serialize' :: FilePath -> IO ()
serialize' = serialize $ getState 0

serialize'' :: FilePath -> FilePath -> IO ()
serialize'' inPath outPath = readState'' inPath >>= \state -> serialize state outPath

getStrings :: State -> [String]
getStrings (strings,_,_) = strings

readStrings' :: IO [String]
readStrings' = getStrings `fmap` readState'

getStrings'' :: FilePath -> [String]
getStrings'' filePath = getStrings $ getState'' filePath

getUserTypes :: State -> [UserType]
getUserTypes (_,uTs,_) = uTs

readUserTypes' :: IO [UserType]
readUserTypes' = getUserTypes `fmap` readState'

getUserTypes'' :: FilePath -> [UserType]
getUserTypes'' filePath = getUserTypes $ getState'' filePath

getTypeDescriptors :: State -> [TD]
getTypeDescriptors (_,_,tDs) = tDs

readTypeDescriptors' :: IO [TD]
readTypeDescriptors' = getTypeDescriptors `fmap` readState'

getTypeDescriptors'' :: FilePath -> [TD]
getTypeDescriptors'' filePath = getTypeDescriptors $ getState'' filePath
