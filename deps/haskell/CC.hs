module CC where

import Data.IORef
import System.IO.Unsafe

myVar :: IORef a -- polymorphic ref!
myVar = unsafePerformIO $ newIORef undefined

coerce :: a -> b
coerce x = unsafePerformIO $ do
    writeIORef myVar x  -- write value of type a
    readIORef myVar     -- read value of type b
