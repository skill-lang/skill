module ImpossibleImports where

import Data.IORef
import System.IO.Unsafe

myVar :: IORef a -- polymorphic ref!
myVar = unsafePerformIO $ newIORef undefined

coerce :: a -> b
coerce x = unsafePerformIO $ (writeIORef myVar x >> readIORef myVar)

modifyIORef' :: IORef a -> (a -> a) -> IO ()
modifyIORef' ref f = do
    x <- readIORef ref
    let x' = f x
    x' `seq` writeIORef ref x'
