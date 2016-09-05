module Methods where

import System.IO as IO
import Data.Word
import Data.Bits as B
import Data.List as L
import Data.ByteString.Lazy.Char8 as C
import Data.ByteString.Lazy as S
import Data.Binary.Get

import Types

select :: Bool -> a -> a -> a
select True x _  = x
select False _ x = x

isGreater :: Maybe Int -> Int -> Bool
isGreater Nothing _  = False
isGreater (Just i) j = i > j

sub :: Maybe Int -> Int -> Maybe Int
sub Nothing _  = Nothing
sub (Just a) b = Just $ a - b

pick :: [String] -> Maybe Int -> Maybe String
pick _ Nothing         = Nothing
pick strings (Just id) = Just $ strings !! id

findStringIndex :: [String] -> String -> Int
findStringIndex [] _ = -1
findStringIndex (string:strings) stringToFind
 | string == stringToFind = 0
 | otherwise              = 1 + findStringIndex strings stringToFind

printList :: Show a => [a] -> IO ()
printList = L.foldr ((>>) . print) (return ())

printStringList :: [String] -> IO ()
printStringList = L.foldr ((>>) . IO.putStrLn) (return ())

printByteStringList :: [ByteString] -> IO ()
printByteStringList = L.foldr ((>>) . C.putStrLn) (return ())

boolToInt :: Bool -> Int
boolToInt True  = 1
boolToInt False = 0

printOrdered :: ([String], [TypeDesc], [String]) -> IO ()
printOrdered (strings, typeDescs, _) = L.foldr ((>>) . printTypeDesc strings) (return ()) typeDescs -- TODO

printTypeDesc :: [String] -> TypeDesc -> IO ()
printTypeDesc strings (name, count, superID, lBPO, fieldDescs)
  = do IO.putStr "-- Type Name: "
       IO.putStrLn name
       IO.putStr "count: "
       print count
       IO.putStr "superID: "
       print superID
       IO.putStr "lbpo: "
       print lBPO
       IO.putStrLn $ select (isJust superID) "This is a new type" "This type extends a previously defined one"
       IO.putStrLn ""
       mapM_ printFieldDesc fieldDescs

isJust :: Maybe a -> Bool
isJust (Just _) = True
isJust Nothing  = False

printFieldDesc :: FieldDesc -> IO ()
printFieldDesc (fieldID, nameID, typeID, data')
  = do IO.putStr "Field ID: "
       print fieldID
       IO.putStr "name ID: "
       print nameID
       IO.putStrLn "Data: "
       printData data'
       IO.putStrLn $ select (isJust typeID) "This is a new field" "This field already exists"
       IO.putStrLn ""


printData :: ByteString -> IO ()
printData data' = printList $ runGet (repeatGet getWord8 (fromIntegral (C.length data'))) data'

repeatGet :: Get a -> Int -> Get [a]
repeatGet getter 0 = return []
repeatGet getter i = getter >>= \d -> repeatGet getter (i-1) >>= \rest -> return (d : rest)


remodel :: [Get a] -> Get [a]
remodel [] = return []
remodel (getter : rest) = getter >>= \a -> remodel rest >>= \r -> return (a : r)

remodel' :: (Get a, Get a) -> Get (a, a)
remodel' (g1, g2) = g1 >>= \v1 -> g2 >>= \v2 -> return (v1, v2)

a = S.append

fI :: Word64 -> Int
fI = fromIntegral


--compress :: Ordered -> ([String], [TypeDesc])
--compress (strings, typeDescs, _) = go strings ([],typeDescs)
--   where go :: [String] -> ([TypeDesc], [TypeDesc]) -> [TypeDesc]
--         go _ (typeDescs, []) = typeDescs
--         go strings (prevTypeDescs, typeDesc : rest) =  go strings (typeDescs, rest)
--              where typeDescs = joinTypeDesc prevTypeDescs typeDesc
--
--
--
--joinTypeDesc :: [TypeDesc] -> TypeDesc -> [TypeDesc]
--joinTypeDesc prevTypeDescs (e1, e2, e3, Just e4, e5) = prevTypeDescs ++ [(e1, e2, e3, Just e4, e5)]
--joinTypeDesc prevTypeDescs (e1, e2, e3, Nothing, e5) = go prevTypeDescs (e1, e2, e3, Nothing, e5)
--  where go (oldTypeDesc : rest) addedTypeDesc
--         | nameID == nameID' = merge oldTypeDesc addedTypeDesc : rest
--         | otherwise         = oldTypeDesc : go rest addedTypeDesc
--               where (nameID, _,_,_,_)  = oldTypeDesc
--                     (nameID', _,_,_,_) = addedTypeDesc
--
--merge :: TypeDesc -> TypeDesc -> TypeDesc
--merge (nameID, count, Just superID, lbpo, fieldDescs) (_, count', _, lbpo', fieldDescs')
--    = (nameID, count + count', Just superID. idk, mergeFieldDescriptors fieldDescs fieldDescs')
--
--mergeFieldDescs :: [FieldDesc] -> [FieldDesc] -> [FieldDesc]
--
  --         go (strings, typeDescs) ((strings', typeDescs') : rest) = go expandedFirst rest
  --             where expandedFirst = (strings ++ strings', P.foldl (includeTypeDescriptor typeDescs) typeDescs'updated)
  --                       where typeDescs'updated = P.map (updateStringRefs (length strings)) typeDescs'

  --updateStringRefs :: Word64 -> TypeDescriptor -> TypeDescriptor
  --updateStringRefs offset (nameID, a,b,c, fieldDescs, d,e)
  --               = (nameID + offset, a,b,c, P.map (\(f,nameID',g,h,i) -> (f,nameID'+offset,g,h,i)) fieldDescs, d,e)


--describe :: IO ()
--describe = do let (strings, typeDesc) = P.head blocks
--              let spacedStrings = L.head strings : L.map (C.append (C.pack ", ")) (L.tail strings)
--              IO.putStr "Strings: >> "
--              mapM_ C.putStr spacedStrings
--              IO.putStrLn " <<"
--              printTypeDesc strings (L.head typeDesc)
--              IO.putStrLn " ... done ... "

--code :: Word8 -> String
--code w' = go w' 0
--   where go w i
--          | i == 8    = []
--          | otherwise = go w (i+1) ++ show (boolToInt $ B.testBit w i)


--codeInt :: Word8 -> Int
--codeInt w' = go w' 0
--   where go w i
--          | i == 8    = 0
--          | otherwise = boolToInt (B.testBit w i) + 2 * go w (i+1)

