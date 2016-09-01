module Stories where

import System.IO as IO
import Data.ByteString.Lazy.Char8 as C
import Data.List as L
import Data.ByteString.Lazy as S
import Data.Int
import Data.Binary.Get as G
import Data.Binary.Put as P
import Data.Char
import System.Random as R
import Data.Word


type Author = (S.ByteString, [S.ByteString])
lineBreak = 36

run = do mainFile           <- C.readFile "C:\\FimFiction\\sourceDollar.txt"
         secondFile         <- C.readFile "C:\\FimFiction\\prevFeatured.txt"
         --let res = S.head $ runGet (getLazyByteString 1) input
         --print res
         let authors         = runGet processMain mainFile
         let sortedAuthors   = L.sortBy (\(a,_) (b,_) -> compare a b) authors
         let prevFeatured    = runGet processSec secondFile
         let filteredAuthors = filterAuthors sortedAuthors prevFeatured
         chosenAuthors      <- randomizeAuthors filteredAuthors []
         chosenStories      <- randomizeStories chosenAuthors
         printResults chosenAuthors chosenStories
         --let allStories      = getAllStories sortedAuthors
         --let allNames        = getAllNames shuffledAuthors
         --printByteStringList allNames
         --C.writeFile "C:\\FimFiction\\processed.txt" $ runPut (putAuthors formatted)
         --print $ L.length sortedAuthors
         --print $ L.length allStories



randomizeAuthors :: [Author] -> [Author] -> IO [Author]
randomizeAuthors all chosen
                        | L.length chosen == 5 = return chosen
                        | otherwise            = do authorIndex <- R.randomRIO (0, L.length all - 1)
                                                    let pick     = all !! authorIndex
                                                    if pick `L.elem` chosen then randomizeAuthors all chosen
                                                                            else randomizeAuthors all (pick : chosen)

randomizeStories :: [Author] -> IO [S.ByteString]
randomizeStories [] = return []
randomizeStories ((_,stories) : authors) = do index <- R.randomRIO (0, L.length stories - 1)
                                              rest <- randomizeStories authors
                                              return ((stories !! index) : rest)

printResults :: [Author] -> [S.ByteString] -> IO ()
printResults [] [] = return ()
printResults ((name,_) : authors) (story : stories) =
             C.putStr story >> IO.putStr " from " >> C.putStrLn name >> printResults authors stories

selectStories :: [Author] -> [Int] -> [S.ByteString]
selectStories [] _ = []
selectStories ((_,stories) : authors) (index : indexes) = (stories !! index) : selectStories authors indexes

getNames :: [Author] -> [S.ByteString]
getNames [] = []
getNames ((name,_) : authors) = name : getNames authors

processMain :: Get [Author]
processMain = do empty <- isEmpty
                 if empty then return [] else
                    do name    <- getLazyByteStringLine
                       stories <- getStories
                       authors <- processMain
                   --if (not . L.null) $ [name] `L.intersect` names then error "ERROR" else
                     -- if (not . L.null) $ allStories `L.intersect` stories then error (L.unlines (L.map C.unpack stories)) else
                       return $ (name, stories) : authors

processSec :: Get [S.ByteString]
processSec = do empty <- isEmpty
                if empty then return [] else do name <- getLazyByteStringLine
                                                rest <- processSec
                                                return (name : rest)

filterAuthors :: [Author] -> [S.ByteString] -> [Author]
filterAuthors [] _ = []
filterAuthors ((name,s) : authors) strings
                             | name `L.elem` strings = filterAuthors authors strings
                             | otherwise             = (name,s) : filterAuthors authors strings

getLazyByteStringLine :: Get S.ByteString
getLazyByteStringLine = do letter <- getWord8
                           if letter == lineBreak then return S.empty else do rest <- getLazyByteStringLine
                                                                              return $ letter `S.cons` rest
getStories :: Get [S.ByteString]
getStories = do line <- getLazyByteStringLine
                if C.unpack line == "---" then return [] else do rest <- getStories
                                                                 return (line : rest)
putAuthors :: [Author] -> Put
putAuthors = L.foldr ((>>) . putAuthor) (return ())

putAuthor :: Author -> Put
putAuthor (name, stories) = P.putLazyByteString name >> P.putLazyByteString (C.pack " --wrote--") >> nL >> putStories stories >> nL

putStories :: [S.ByteString] -> Put
putStories [] = P.flush
putStories (story : stories) = P.putLazyByteString story >> nL >> putStories stories

getAllStories :: [Author] -> [S.ByteString]
getAllStories [] = []
getAllStories ((_,stories) : authors) = stories ++ getAllStories authors

getAllNames :: [Author] -> [S.ByteString]
getAllNames [] = []
getAllNames ((name,_) : authors) = name : getAllNames authors

nL :: Put
nL = P.putWord8 lineBreak