module ReadFields where

import qualified Data.ByteString.Lazy.Char8 as C
import qualified Control.Applicative as A
import Control.Monad as V
import Data.ByteString.Lazy as S
import Data.Bits as B
import Data.List as L
import Data.Binary.Get as G
import Data.Char
import Data.Int
import Data.Word
import Methods
import ImpossibleImports
import Memory

g'bool :: Get Bool
g'bool = (/= 0) `fmap` getWord8

g'fixedLengthArray :: Get a -> Int -> Get [a]
g'fixedLengthArray _ 0 = return []
g'fixedLengthArray f i = f >>= \first -> g'fixedLengthArray f (i-1) >>= \rest -> return (first : rest)

g'variableLengthArray :: Get a -> Get [a]
g'variableLengthArray f = g'v64 >>= g'fixedLengthArray f

g'string :: [String] -> Get String
g'string strings = (\i -> strings !! i) `fmap` g'index

parseTypeDescription :: [String] -> Get (ByteString, Get Something)
parseTypeDescription s = do b1           <- lookAhead $ g'v64ByteString
                            num          <- g'v64
                            (b2, getter) <- go s num
                            return (b1 `a` b2, getter)
 where go _ 0  = do b <- lookAhead $ getLazyByteString 1
                    v <- getInt8
                    return (b, (return (CInt8 v)))
       go _ 1  = do b <- lookAhead $ getLazyByteString 2
                    v <- getInt16be
                    return (b, (return (CInt16 v)))
       go _ 2  = do b <- lookAhead $ getLazyByteString 4
                    v <- getInt32be
                    return (b, (return (CInt32 v)))
       go _ 3  = do b <- lookAhead $ getLazyByteString 8
                    v <- getInt64be
                    return (b, (return (CInt64 v)))
       go _ 4  = do b <- lookAhead $ g'v64ByteString
                    v <- g'v64Signed
                    return (b, (return (CV64 v)))
       go _ 5  = return (S.empty, GRef `fmap` g'indexPair) -- TODO verify
       go _ 6  = return (S.empty, GBool `fmap` g'bool)     -- 0-4 g's the value directly
       go _ 7  = return (S.empty, GInt8 `fmap` getInt8)
       go _ 8  = return (S.empty, GInt16 `fmap` getInt16be)
       go _ 9  = return (S.empty, GInt32 `fmap` getInt32be)
       go _ 10 = return (S.empty, GInt64 `fmap` getInt64be)
       go _ 11 = do return (S.empty, GV64 `fmap` g'v64Signed)
       go _ 12 = do return (S.empty, GFloat `fmap` g'float)
       go _ 13 = do return (S.empty, GDouble `fmap` g'double)
       go _ 14 = do return (S.empty, GString `fmap` g'string s)
       go s 15 = do n           <- g'v64
                    (b, action) <- parseTypeDescription s
                    let getter = remodel $ L.replicate n action
                    return (b, GFArray `fmap` getter)
       go s 17 = do (b, action)  <- parseTypeDescription s
                    return (b, (\g -> g'v64 >>= \i -> GVArray `fmap` repeatGet i g) action)
       go s 18 = do (b, action)  <- parseTypeDescription s
                    return (b, (\g -> g'v64 >>= \i -> GList `fmap` repeatGet i g) action)
       go s 19 = do (b, action)  <- parseTypeDescription s
                    return (b, (\g -> g'v64 >>= \i -> GSet `fmap` repeatGet i g) action)
       go s 20 = do (b1, g1) <- parseTypeDescription s
                    (b2, g2) <- parseTypeDescription s
                    let getMap = (\(g1, g2) -> g'v64 >>= \i -> GMap `fmap` repeatGet i (remodel' (g1, g2))) (g1, g2)
                    --op :: g'v64 >>= (Get a, Get b) -> Get [(a,b)] with length v64
                    return (b1 `a` b2, getMap)
       go _ n  = do return (S.empty, ((\index -> GRef ((n - 32), index)) `fmap` g'index))

g'float :: Get Float
g'float = coerce `fmap` getWord32be

g'double :: Get Double
g'double = coerce `fmap` getWord64be

g'index :: Get Int
g'index = subIndex `fmap` g'v64

g'refString :: [String] -> Get String
g'refString strings = g'index >>= \i -> return $ strings !! i

maybeRead :: Bool -> Get a -> Get (Maybe a)
maybeRead False _     = return Nothing
maybeRead True getter = Just `fmap` getter

g'v64 :: Get Int
g'v64 = calc `fmap` g'v64ByteString

calc :: ByteString -> Int
calc bytestring = go (S.unpack bytestring) 1
           where go :: [Word8] -> Int -> Int
                 go [] _ = 0
                 go (word : words) count
                  | L.null words && count == 9 = 2 * fromIntegral word -- L.null überflüssig?
                  | otherwise                  = B.clearBit (fromIntegral word) 7 + 128 * go words (count+1)


g'v64Pair :: Get (Int, Int)
g'v64Pair = g'v64 >>= \a -> g'v64 >>= \b -> return (a, b)

g'indexPair :: Get (Int, Int)
g'indexPair = do a <- g'index
                 b <- g'index
                 return (a, b)

g'v64Signed :: Get Int64
g'v64Signed = fromIntegral `fmap` g'v64


skipV64 :: Get ()
skipV64 = V.void g'v64

g'v64ByteString :: Get ByteString
g'v64ByteString = go 1
   where go i = do byte <- getWord8
                   if i == 9 || not (testBit byte 7)
                     then return $ S.singleton byte
                     else go (i+1) >>= \rest -> return $ S.singleton byte `a` rest

-- shouldRead -> Maybe ByteString
g'm'typeRestrictions :: Bool -> Get (Maybe ByteString)
g'm'typeRestrictions False = return Nothing
g'm'typeRestrictions True  = do info <- g'v64ByteString
                                let num = calc info
                                rest <- g'typeRestrictions num
                                return (Just (info `a` rest))

-- numToRead -> byteString
g'typeRestrictions :: Int -> Get ByteString
g'typeRestrictions 0 = return empty
g'typeRestrictions c = do info  <- g'v64ByteString
                          let id = calc info
                          restr <- g'typeRestriction id
                          rest  <- g'typeRestrictions (c-1)
                          return (info `a` restr `a` rest)

-- restrictionID -> ByteString
g'typeRestriction :: Int -> Get ByteString
g'typeRestriction 5 = g'v64ByteString
g'typeRestriction _ = return empty

skipType :: Int -> Get ()
skipType id
         | id == 0 || id == 7 || id == 6              = skip 1
         | id == 1 || id == 8                         = skip 2
         | id == 2 || id == 9                         = skip 4
         | id == 3 || id == 10                        = skip 8
         | id == 4 || id == 5 || id == 11 || id == 14 = skipV64


-- shouldRead -> typeID -> ()
g'm'fieldRestrictions :: Bool -> Maybe (Get Something) -> Get (Maybe ByteString)
g'm'fieldRestrictions False _            = return Nothing
g'm'fieldRestrictions True (Just getter) = do info <- g'v64ByteString
                                              let num = calc info
                                              rest <- g'fieldRestrictions num getter
                                              return (Just (info `a` rest))


-- numToRad -> typeID -> ()
g'fieldRestrictions :: Int -> Get Something -> Get ByteString
g'fieldRestrictions 0 _      = return empty
g'fieldRestrictions c getter = do info <- g'v64ByteString
                                  let id = calc info
                                  next <- g'fieldRestriction id getter
                                  rest <- g'fieldRestrictions (c-1) getter
                                  return (info `a` next `a` rest)

-- restrictionID -> typeID -> ()
g'fieldRestriction :: Int -> Get Something -> Get ByteString
g'fieldRestriction 1 getter = g'bytes getter
g'fieldRestriction 3 getter = g'bytes getter >>= \b1 -> g'bytes getter >>= \b2 -> return (b1 `a` b2)
g'fieldRestriction 5 getter = g'bytes g'v64
--g'fieldRestriction 9 getter = void $ g'variableLengthArray (parseTypeDescription []) TODO
g'fieldRestriction _ _      = return empty
--
--
g'bytes :: Get a -> Get ByteString
g'bytes getter = do bytesRead1 <- bytesRead
                    bytesRead2 <- lookAhead (getter A.*> bytesRead)
                    getLazyByteString $ fromIntegral (bytesRead2 - bytesRead1)
