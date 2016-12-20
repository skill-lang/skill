module Methods where

import qualified Data.ByteString.Lazy.Char8 as C
import qualified Data.ByteString.Lazy as L
import Data.List
import System.IO
import Data.Word
import Data.Binary.Get
import Data.Int
import Memory

assure :: Maybe a -> a
assure (Just a) = a
assure _        = error "Error: can't convert Nothing to value"

assure' :: String -> Maybe a -> a
assure' _ (Just a) = a
assure' errorMsg _ = error errorMsg

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

deleteAt :: Int -> [a] -> [a]
deleteAt index list = take index list ++ tail (drop index list)

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
printStringList = foldr ((>>) . putStrLn) (return ())

printByteStringList :: [L.ByteString] -> IO ()
printByteStringList list = putStr "[" >> foldr ((>>) . C.putStr) (return ()) (intersperse (C.pack ", ") list) >> putStr "]"

boolToInt :: Bool -> Int
boolToInt True  = 1
boolToInt False = 0

printState :: ([String], [String], [TD]) -> IO ()
printState (strings, _, tDs) = printTypeDescs tDs >> eL 2

printState' :: ([String], [String], [TD]) -> IO ()
printState' (strings, _, tDs) = mapM_ print strings >> putStrLn "%%%%%%%%" >> printTypeDescs tDs >> eL 2

tDs'toString :: [TD] -> String
tDs'toString [] = []
tDs'toString ((TD id name count fieldDescs subtypes _) : r_tDs)
            = "   ### Type " ++ show id ++ " : " ++ name ++ " ###\n" ++ concatMap fDs'toString fieldDescs
              ++ "\nNumber of Subtypes: " ++ show (length subtypes) ++ tDs'toString subtypes >> tDs'toString r_tDs
fDs'toString :: FD -> String
fDs'toString (name, d, (rawD, _, _)) = "      > Field " ++ name ++ ": \n" ++ d'toString rawD
                                             ++ " -> " ++ list'toString d ++ "\n"
list'toString :: Show a => [a] -> String
list'toString []   = "[]"
list'toString list = "[" ++ go (init list) ++ (show . last) list ++ "]"
   where go [] = ""
         go (x : xs) = show x ++ ", " ++ go xs
d'toString :: L.ByteString -> String
d'toString d = list'toString (runGet getter d)
   where getter = repeatGet ((fromIntegral . C.length) d) getWord8


printTypeDescs :: [TD] -> IO ()
printTypeDescs [] = return ()
printTypeDescs ((TD id name count fDs subtypes _) : r_tDs)
            = putStrLn ("   ### Type " ++ show id ++ " : " ++ name ++ " ###") >> eL 1 >> mapM_ printFD fDs
              >> eL 1 >> putStr "Number of Subtypes: " >> print (length subtypes) >> printTypeDescs subtypes >> printTypeDescs r_tDs

printFD :: FD -> IO ()
printFD (name, d, (rawD, _, _)) = putStr ("      > Field " ++ name ++ ": ") >> printData rawD
                                             >> putStr " -> " >> printList d >> eL 1

printData :: L.ByteString -> IO ()
printData d' = printList (runGet getter d')
   where getter = repeatGet ((fromIntegral . C.length) d') getWord8

convertData :: L.ByteString -> [Word8]
convertData d = runGet getter d
   where getter = repeatGet ((fromIntegral . C.length) d) getWord8

printRealData :: Get [Something] -> L.ByteString -> IO ()
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

-- replaces an element at a specific index in a list, returns the changed list
replace' :: Int -> a -> [a] -> [a]
replace' _ _ [] = []
replace' 0 newElem (x : r_list) = newElem : r_list
replace' i newElem (x : r_list) = x : replace' (i-1) newElem r_list

replaceSection :: Int -> Int -> a -> [a] -> [a]
replaceSection 0 count newElem list         = replicate count newElem ++ drop count list
replaceSection i count newElem (e : r_list) = e : replaceSection (i-1) count newElem r_list

-- cutSlice [1,2,3,4,5,6,7,8,9] (3, 5) = ([1,2,3,9],[4,5,6,7,8])
cutSlice :: [a] -> (Int, Int) -> ([a], [a])
cutSlice xs (offset, count) = (xsA1 ++ xsA2, xsB)
    where (xsA1, xsR) = splitAt offset xs
          (xsB, xsA2) = splitAt count xsR

splitGet :: (Int, Int) -> Get [a] -> (Get [a], Get [a])
splitGet (offset, count) getter = (dropMid offset count `fmap` getter, subList offset count `fmap` getter)

subList :: Int -> Int -> [a] -> [a]
subList offset count = take count . drop offset

dropMid :: Int -> Int -> [a] -> [a]
dropMid offset count list = take offset list ++ drop (count + offset) list

appendMaybe :: Maybe [a] -> [a] -> [a]
appendMaybe Nothing = id
appendMaybe (Just list1) = (++) list1

-- replaces element at a given index in a list
r :: Int -> a -> [a] -> [a]
r index elem list = take index list ++ [elem] ++ drop (index + 1) list

-- follows a pointer to its target
-- this procedure operates on two modes, starting on mode 1
-- in mode 1, search for the tD with the correct id (as given by (fst ref))
-- in mode 2, go back downward through the respective subtree, searching for the right instance
-- the mode is signaled by the attribute ssc; ssc < 0 -> enter mode 2 (and stay there)
-- until then ssc equals the sum of the counts of all local super types
reach :: Ref -> [TD] -> ([FD], String, Int)
reach ref tDs = (fDs, name, pos)
  where (TD id name count fDs s_tDs rec, pos) = assure $ go 0 ref tDs
        go :: Int -> (Int, Int) -> [TD] -> (Maybe (TD, Int))
        go _ _ [] = Nothing
        go (-1) (_, i2) (TD id name count fDs s_tDs record : r_f_tDs)                           -- ||| Mode 2 |||
         | i2 < count = Just $ (TD id name count fDs s_tDs record, i2)                          -- index applies locally -> SUCCESS
         | otherwise  = case (go (-1) (undefined, i2 - count) s_tDs)                                   -- index too high -> subtract it and call downward
                 of Just res -> Just res
                    Nothing  -> case (go (-1) (undefined, i2 - count) r_f_tDs) of Just res -> Just res -- call forward
                                                                                  Nothing  -> Nothing
        go ssc (i1, i2) (TD id name count fDs s_tDs record : r_f_tDs)                           -- ||| Mode 1 |||
         | id == i1  = go (-1) (undefined, i2 - ssc) [TD id name count fDs s_tDs record]      -- found tD with id -> SWITCH TO MODE 2, self-call
         | otherwise = case (go (ssc + count) (i1, i2) s_tDs)                                          -- didn't find tD yet -> keep looking, call downward
                 of Just res -> Just res
                    Nothing  -> case (go ssc (i1, i2) s_tDs) of Just res -> Just res                   -- call forward
                                                                Nothing  -> Nothing
lengthBS :: L.ByteString -> Int
lengthBS = fromIntegral . L.length

fst' (a,b,c) = a
snd' (a,b,c) = b
trd' (a,b,c) = c
fst'' (a,b,c,d) = a
snd'' (a,b,c,d) = b
trd'' (a,b,c,d) = c
fth'' (a,b,c,d) = d

-- ghci macro
p :: String -> String
p name = "C:/input/" ++ name ++ ".sf"

getFDs :: TD -> [FD]
getFDs (TD _ _ _ fDs _ _) = fDs

a = L.append
fI :: Word64 -> Int
fI = fromIntegral
