package de.ust.skill.generator.haskell

/**
 * creates the package interface
 */
trait InterfaceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    val out = open("Interfaces.hs")

    out.write(s"""
-----------------------------------------------
--Access the Binding by writing a method here--
-------Or by opening this Module in GHCI-------
-----------------------------------------------
module Interface where

import Controls
import Memory
import Deserialize
import Serialize
import ImpossibleImports
import Methods
import Follow
import Types
import ReadFields
import WriteFields
import Test.HUnit;
${IR.map(t ⇒ s"\nimport Access${t.getName().capital()}").mkString}

main :: IO ()
main = putStr "Write Procedure Here"
""")

    out.close()
  }
}