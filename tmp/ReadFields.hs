module ReadFields where

import Control.Monad as V
import Data.ByteString.Lazy as S
import Data.Bits as B
import Data.List as L
import Data.ByteString.Lazy.Char8 as C
import Data.Binary.Get as G
import Data.Bits.Floating
import Data.Char
import Data.Int
import Data.Word
import Methods
import Types

readBool :: Get Bool
readBool = (/= 0) `fmap` getWord8

readFixedLengthArray :: Get a -> Int -> Get [a]
readFixedLengthArray _ 0 = return []
readFixedLengthArray f i = f >>= \first -> readFixedLengthArray f (i-1) >>= \rest -> return (first : rest)

readVariableLengthArray :: Get a -> Get [a]
readVariableLengthArray f = readV64 >>= readFixedLengthArray f

readString :: [S.ByteString] -> Get String
readString strings = readV64 >>= \n -> if n == 0 then return "" else return $ C.unpack $ strings !! (n - 1)

--mA :: Get (Get Word8)
--mA = return (id `fmap` getWord8)

--mB :: Get (Get Word8)
--mB = return `fmap` getWord8


parseTypeDescription :: Get (Get Something)
parseTypeDescription = readV64 >>= go
   where go 0  = (return . CInt8)  `fmap` getInt8
         go 1  = (return . CInt16) `fmap` getInt16be
         go 2  = (return . CInt32) `fmap` getInt32be
         go 3  = (return . CInt64) `fmap` getInt64be
         go 4  = (return . CV64)   `fmap` readV64Signed
         go 5  = return (GPointer `fmap` readV64Pair)  -- Note: the syntax of 0-4 and 5-14 are \not\ interchangeable
         go 6  = return (GBool    `fmap` readBool)     -- 0-4 reads the value directly
         go 7  = return (GInt8    `fmap` getInt8)      -- 5-14 does not until the inner Get is run
         go 8  = return (GInt16   `fmap` getInt16be)   -- see NestedGetExample for an illustration
         go 9  = return (GInt32   `fmap` getInt32be)
         go 10 = return (GInt64   `fmap` getInt64be)
         go 11 = return (GV64     `fmap` readV64Signed)
         go 12 = return (GFloat   `fmap` readFloat)
         go 13 = return (GDouble  `fmap` readDouble)
         go 14 = return (GString  `fmap` readV64)
         go 15 = do n         <- readV64
                    action    <- parseTypeDescription
                    let getter = remodel $ L.replicate n action
                    return (GFArray `fmap` getter)
         go 17 = (\g        -> readV64 >>= \i -> GVArray `fmap` repeatGet i g) `fmap` parseTypeDescription
         go 18 = (\g        -> readV64 >>= \i -> GList   `fmap` repeatGet i g) `fmap` parseTypeDescription
         go 19 = (\g        -> readV64 >>= \i -> GSet    `fmap` repeatGet i g) `fmap` parseTypeDescription
         go 20 = (\(g1, g2) -> readV64 >>= \i -> GMap    `fmap` repeatGet i (remodel' (g1, g2))) `fmap` doubleGetter
                        where doubleGetter = remodel' (parseTypeDescription, parseTypeDescription)
         go n  = return (GUserType (n - 32) `fmap` readV64)

readFloat :: Get Float
readFloat = coerceToFloat `fmap` getWord32be

readDouble :: Get Double
readDouble = coerceToFloat `fmap` getWord64be

readIndex :: Get Int
readIndex = subIndex `fmap` readV64

readRefString :: [String] -> Get String
readRefString strings = readIndex >>= \i -> return $ strings !! i

maybeRead :: Bool -> Get a -> Get (Maybe a)
maybeRead False _     = return Nothing
maybeRead True getter = Just `fmap` getter

readV64 :: Get Int
readV64 = calc `fmap` readV64ByteString 1
 where calc bytestring = go (S.unpack bytestring) 1
           where go :: [Word8] -> Int -> Int
                 go [] _ = 0
                 go (word : words) count
                  | L.null words && count == 9 = 2 * fromIntegral word -- L.null überflüssig?
                  | otherwise                  = B.clearBit (fromIntegral word) 7 + 128 * go words (count+1)


readV64Pair :: Get (Int, Int)
readV64Pair = readV64 >>= \a -> readV64 >>= \b -> return (a, b)

readV64Signed :: Get Int64
readV64Signed = fromIntegral `fmap` readV64


skipV64 :: Get ()
skipV64 = V.void readV64

readV64ByteString :: Word8 -> Get S.ByteString
readV64ByteString i = do byte <- getWord8
                         if i == 9 || not (testBit byte 7) then return $ S.singleton byte
                         else readV64ByteString (i+1) >>= \rest -> return $ S.singleton byte `a` rest

-- shouldRead -> ()
maybeSkipTypeRestrictions :: Bool -> Get ()
maybeSkipTypeRestrictions False = return ()
maybeSkipTypeRestrictions True  = readV64 >>= skipTypeRestrictions

-- numToRead -> ()
skipTypeRestrictions :: Int -> Get ()
skipTypeRestrictions 0 = return ()
skipTypeRestrictions c = readV64 >>= skipTypeRestriction >> skipTypeRestrictions (c-1)

-- restrictionID -> ()
skipTypeRestriction :: Int -> Get ()
skipTypeRestriction 5 = skipV64
skipTypeRestriction _ = return ()

skipType :: Int -> Get ()
skipType id
         | id == 0 || id == 7 || id == 6              = skip 1
         | id == 1 || id == 8                         = skip 2
         | id == 2 || id == 9                         = skip 4
         | id == 3 || id == 10                        = skip 8
         | id == 4 || id == 5 || id == 11 || id == 14 = skipV64


-- shouldRead -> typeID -> ()
maybeSkipFieldRestrictions :: Bool -> Maybe (Get Something) -> Get ()
maybeSkipFieldRestrictions False _            = return ()
maybeSkipFieldRestrictions True (Just getter) = readV64 >>= \count -> skipFieldRestrictions count getter

-- numToRad -> typeID -> ()
skipFieldRestrictions :: Int -> Get Something -> Get ()
skipFieldRestrictions 0 _      = return ()
skipFieldRestrictions c getter = readV64 >>= \rID -> skipFieldRestriction rID getter >> skipFieldRestrictions (c-1) getter

-- restrictionID -> typeID -> ()
skipFieldRestriction :: Int -> Get Something -> Get ()
skipFieldRestriction 1 getter = skipGetSomething getter
skipFieldRestriction 3 getter = skipGetSomething getter >> skipGetSomething getter
skipFieldRestriction 5 getter = void readV64
skipFieldRestriction 9 getter = void $ readVariableLengthArray parseTypeDescription
skipFieldRestriction _ _      = return ()




skipGetSomething :: Get Something -> Get ()
skipGetSomething getter = getter >>= go
   where go (GVArray x) = error "TODO: Variable Length Errors in Restrictions" -- TODO
         go (GList   x) = error "TODO: Variable Length Errors in Restrictions" -- TODO
         go (GSet    x) = error "TODO: Variable Length Errors in Restrictions" -- TODO
         go getter      = return ()