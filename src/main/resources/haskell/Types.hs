module Types where

import Data.Int
import D_Types

data I_A = A'A A | A'B B | A'C C | A'D D | A'E E
type A = (String)
data I_B = B'B B | B'D D | B'E E
type B = (String, Int16)
data I_D = D'D D | D'E E
type D = (String, Int16, Int64)
data I_E = E'E E
type E = (String, Int16, Int64, Int64)
data I_C = C'C C
type C = (String, Int32)
data I_F = F'F F
type F = (Maybe A)
data I_G = G'G G
type G = (Maybe B)
data I_H = H'H H
type H = (Maybe C)

c'A_A (GString value) = value
c'B_B (GInt16 value) = value
c'D_D (GInt64 value) = value
c'E_E (GV64 value) = value
c'C_C (GInt32 value) = value