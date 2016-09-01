module Editing where

import System.IO
import Data.List
import GHC.IO.Encoding




run = setLocaleEncoding utf8 >> readFile "input.txt" >>= \txt -> writeFile "output.txt" (map replace (transform txt))

replace :: (Char, Char, Char) -> Char
replace (_, '\'', _)   = '’'
replace (' ', '"', _)  = '“'
replace ('\n', '"', _) = '“'
replace (_, '"', ' ')  = '”'
replace (_, '"', '\n') = '”'
replace (_, a, _)      = a


transform :: String -> [(Char, Char, Char)]
transform (a : b : rest) = [(' ', a, b)] ++ go (a : b : rest) ++ [(y, z, ' ')]
    where z      = last rest
          y      = (last . init) rest
          go (a : b : c : rest) = (a, b, c) : go (b : c : rest)
          go _                  = []

