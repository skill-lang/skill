module Methods where

import qualified Data.ByteString.Lazy.Char8 as C
import qualified Data.ByteString.Lazy as S
import System.IO
import Data.Word
import Data.Bits as B
import Data.List as L
import Data.Binary.Get
import Types

select :: Bool -> a -> a -> a
select True x _  = x
select False _ x = x

isGreater :: Maybe Int -> Int -> Bool
isGreater Nothing _  = False
isGreater (Just a) b = a > b

sub :: Maybe Int -> Int -> Maybe Int
sub Nothing _  = Nothing
sub (Just a) b = Just $ a - b

pick :: [String] -> Maybe Int -> Maybe String
pick _ Nothing        = Nothing
pick strings (Just i) = Just $ strings !! i

findStringIndex :: [String] -> String -> Int
findStringIndex [] _ = -1
findStringIndex (string : r_strings) stringToFind
 | string == stringToFind = 0
 | otherwise              = 1 + findStringIndex r_strings stringToFind

printList :: Show a => [a] -> IO ()
printList []   = putStr "[]"
printList list = putStr "[" >> go (init list) >> (putStr . show . last) list >> putStr "]"
   where go [] = return ()
         go (x : xs) = (putStr . show) x >> putStr ", " >> go xs

printStringList :: [String] -> IO ()
printStringList = L.foldr ((>>) . putStrLn) (return ())

printByteStringList :: [S.ByteString] -> IO ()
printByteStringList list = putStr "[" >> L.foldr ((>>) . C.putStr) (return ()) (intersperse (C.pack ", ") list) >> putStr "]"


boolToInt :: Bool -> Int
boolToInt True  = 1
boolToInt False = 0

printOrdered :: ([String], [TypeDesc], [String]) -> IO ()
printOrdered (strings, typeDescs, _) = mapM_ printTypeDesc typeDescs >> eL 2

printTypeDesc :: TypeDesc -> IO ()
printTypeDesc (TD (id, name, fieldDescs, subtypes))
            = putStrLn ("   ### Type " ++ show id ++ " : " ++ name ++ " ###") >> eL 1 >> mapM_ printFieldDesc fieldDescs
              >> eL 1 >> putStr "Number of Subtypes: " >> print (L.length subtypes) >> mapM_ printTypeDesc subtypes

printFieldDesc :: FieldDescTest -> IO ()
printFieldDesc (name, data', rawData) = putStr ("      > Field " ++ name ++ ": ") >> printData rawData
                                     >> putStr " -> " >> printList data' >> eL 1

printData :: S.ByteString -> IO ()
printData data' = printList (runGet getter data')
   where getter = repeatGet ((fromIntegral . C.length) data') getWord8

printRealData :: Get [Something] -> S.ByteString -> IO ()
printRealData getter string = printList (runGet getter string) >> eL 1

printTestName :: String -> IO ()
printTestName string = eL 2 >> putStrLn minuses >> putStrLn ("-- Test " ++ string ++ " --") >> putStrLn minuses
   where minuses = replicate (11 + length string) '-'

eL :: Int -> IO ()
eL 0 = return ()
eL i = putStrLn "" >> eL (i-1)

isJust :: Maybe a -> Bool
isJust (Just _) = True
isJust Nothing  = False

subIndex :: Int -> Int
subIndex i = i-1

repeatGet :: Int -> Get a -> Get [a]
repeatGet 0 _      = return []
repeatGet i getter = getter >>= \d -> repeatGet (i-1) getter >>= \rest -> return (d : rest)

remodel :: [Get a] -> Get [a]
remodel [] = return []
remodel (getter : rest) = getter >>= \a -> remodel rest >>= \r -> return (a : r)

remodel' :: (Get a, Get a) -> Get (a, a)
remodel' (g1, g2) = g1 >>= \v1 -> g2 >>= \v2 -> return (v1, v2)

remodel'' :: [(a, b)] -> ([a], [b])
remodel'' = L.foldr (\(a,b) (lA, lB) -> (a : lA, b : lB)) ([], [])

replace' :: Int -> a -> [a] -> [a]
replace' 0 newElem (x : r_list) = newElem : r_list
replace' i newElem (x : r_list) = x : replace' (i-1) newElem r_list

replaceSection :: Int -> Int -> a -> [a] -> [a]
replaceSection 0 count newElem list         = L.replicate count newElem ++ L.drop count list
replaceSection i count newElem (e : r_list) = e : replaceSection (i-1) count newElem r_list

-- cutSlice [1,2,3,4,5,6,7,8,9] (3, 5) = ([1,2,3,9],[4,5,6,7,8])
cutSlice :: [a] -> (Int, Int) -> ([a], [a])
cutSlice xs (offset, count) = (xsA1 ++ xsA2, xsB)
    where (xsA1, xsR) = L.splitAt offset xs
          (xsB, xsA2) = L.splitAt count xsR

splitGet :: (Int, Int) -> Get [a] -> (Get [a], Get [a])
splitGet (offset, count) getter = (dropMid offset count `fmap` getter, subList offset count `fmap` getter)

subList :: Int -> Int -> [a] -> [a]
subList offset count = L.take count . L.drop offset

dropMid :: Int -> Int -> [a] -> [a]
dropMid offset count list = L.take offset list ++ L.drop (count + offset) list

appendMaybe :: Maybe [a] -> [a] -> [a]
appendMaybe Nothing = id
appendMaybe (Just list1) = (++) list1

a = S.append
fI :: Word64 -> Int
fI = fromIntegral