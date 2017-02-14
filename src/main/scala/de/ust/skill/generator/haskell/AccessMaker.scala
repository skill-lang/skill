package de.ust.skill.generator.haskell

import scala.collection.JavaConversions._

import de.ust.skill.ir.Field
import de.ust.skill.ir.UserType
import de.ust.skill.ir.ContainerType

/**
 * creates the package interface
 */
trait AccessMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      val n = name(t);

      val out = files.open(s"Access$n.hs")

      out.write(s"""module Access$n where

import qualified Data.Map as M
import Types
import Deserialize
import Controls
import Memory
import Methods
import Follow
import Data.Int
import Data.IORef
import System.IO.Unsafe

get${n}s :: State -> [$n]
get${n}s = go . getTypeDescriptors
 where go [] = []
       go ((TD id name count fDs subTypes _) : rest)
            | name == "${t.getSkillName}" && (not . null) fDs = go' (0, count) fDs
            | not $$ null (go subTypes) = go subTypes
            | otherwise = go rest
               where go' :: (Int, Int) -> [FD] -> [$n]
                     go' _ [] = []
                     go' (id, size) fDs
                         | id == size = []
                         | otherwise  = (id `extract$n` fDs) : go' (id + 1, size) fDs

read${n}s :: Int -> IO [$n]
read${n}s index = get${n}s `fmap` readState index

read${n}s' :: IO [$n]
read${n}s' = get${n}s `fmap` readState'

get${n}s'' :: FilePath -> [$n]
get${n}s'' filePath = get${n}s $$ (unsafePerformIO . deserialize_) filePath

get${n} :: State -> Int -> $n
get${n} state index = assure $$ go (getTypeDescriptors state) index
 where go :: [TD] -> Int -> Maybe $n
       go [] _ = Nothing
       go ((TD id name count fDs subTypes _) : rest) index
            | name == "${t.getSkillName}" && (not . null) fDs = Just $$ index `extract$n` fDs
            | isJust (go subTypes index) = go subTypes index
            | otherwise = go rest index

get${n}' :: Int -> $n
get${n}' = get${n} $$ getState 0

get${n}'' :: FilePath -> Int -> $n
get${n}'' filePath index  = unsafePerformIO $$ read${n}'' filePath index

read${n}' :: Int -> IO $n
read${n}' index = (\\state -> (get${n} state index)) `fmap` readState'

read${n}'' :: FilePath -> Int -> IO $n
read${n}'' filePath index = deserialize filePath >> read${n}' index
""")

      locally {
        //the tuple representation of an instance
        val repr = (0 until t.getAllFields.size).map(i ⇒ "f" + i).mkString("(", ", ", ")")

        for ((f, i) ← t.getAllFields.zipWithIndex) {
          val ft = mapType(f.getType, true) + (f.getType match {
            case t : UserType if isReference(f) && !t.getSubTypes.isEmpty ⇒ "'"
            case _ ⇒ ""
          })

          val followPrefix =
            if (isReference(f)) s"follow${name(f)} (getTypeDescriptors $$ getState 0)"
            else ""

          out.write(s"""
get${n}_${name(f)} :: $n -> $ft
get${n}_${name(f)} $repr = $followPrefix f$i

get${n}_${name(f)}s :: State -> [$ft]
get${n}_${name(f)}s state = map get${n}_${name(f)} (get${n}s state)

get${n}_${name(f)}s'' :: FilePath -> [$ft]
get${n}_${name(f)}s'' filePath = get${n}_${name(f)}s $$ (unsafePerformIO . deserialize_) filePath
""")
        }
      }

      locally {

        val fields = t.getAllFields
        val range = (0 until fields.size)

        val where1_pre = range.map(i ⇒ s"g$i").mkString("(", ", ", ")")
        val fieldSignature = range.map(i ⇒ s"(n$i, f$i, (d$i, t$i, r$i))").mkString("[", ", ", "]")
        val objectSignature = range.map(i ⇒ s"o$i").mkString("(", ", ", ")")
        val returnStatement = range.map(i ⇒ s"(n$i, g$i, (d$i, t$i, r$i))").mkString("[", ", ", "]")

        out.write(s"""
set$n :: Int -> Int -> $n -> IO ()
set$n sI i obj = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs i obj))
  where go :: [TD] -> Int -> $n -> Maybe [TD]
        go [] _ _ = Nothing
        go (tD : r_tDs) i i_c
            | name == "${t.getSkillName}" = Just (TD e1 name e3 (go' fDs i i_c) subtypes e6 : r_tDs)
            | otherwise = case go subtypes i i_c                                         -- call downward
                of (Just tDs') -> Just (TD e1 name e3 fDs tDs' e6 : r_tDs)
                   Nothing -> case go r_tDs i i_c of (Just tDs') -> Just (tD : tDs')  -- call forward
                                                     Nothing     -> Nothing
                   where TD e1 name e3 fDs subtypes e6 = tD

        go' :: [FD] -> Int -> $n -> [FD]
        go' $fieldSignature i $objectSignature
          = $returnStatement
                where $where1_pre = ${range.map(i ⇒ s"replace' i x$i f$i").mkString("(", ", ", ")")}
                      ${range.map(i ⇒ s"x$i").mkString("(", ", ", ")")} = ${
          fields.zipWithIndex.map { case (f, i) ⇒ s"${BoxedDataConstructor(f)} o$i" }.mkString("(", ", ", ")")
        }

set$n' :: Int -> $n -> IO ()
set$n' = set$n 0

make$n :: Int -> $n -> IO ()
make$n sI obj = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs obj))
  where go :: [TD] -> $n -> Maybe [TD]
        go [] _ = Nothing
        go (tD : r_tDs) i_c
            | name == "${t.getSkillName}" = Just (TD e1 name (count + 1) (go' fDs i_c) subtypes e6 : r_tDs)
            | otherwise = case go subtypes i_c                                         -- call downward
                of (Just tDs') -> Just (TD e1 name count fDs tDs' e6 : r_tDs)
                   Nothing -> case go r_tDs i_c of (Just tDs') -> Just (tD : tDs')  -- call forward
                                                   Nothing     -> Nothing
                   where TD e1 name count fDs subtypes e6 = tD

        go' :: [FD] -> $n -> [FD]
        go' $fieldSignature $objectSignature
          = $returnStatement
                where $where1_pre = ${
          fields.zipWithIndex.map {
            case (f, i) ⇒ s"f$i ++ [${BoxedDataConstructor(f)} o$i]"
          }.mkString("(", ", ", ")")
        }

make$n' :: $n -> IO ()
make$n' = make$n 0

delete$n :: Int -> Int -> IO ()
delete$n sI i = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs i))
  where go :: [TD] -> Int -> Maybe [TD]
        go [] _ = Nothing
        go (tD : r_tDs) i
            | name == "${t.getSkillName}" = Just (TD e1 name (count - 1) (go' fDs i) subtypes e6 : r_tDs)
            | otherwise = case go subtypes i                                         -- call downward
                of (Just tDs') -> Just (TD e1 name count fDs tDs' e6 : r_tDs)
                   Nothing -> case go r_tDs i of (Just tDs') -> Just (tD : tDs')  -- call forward
                                                 Nothing     -> Nothing
                   where TD e1 name count fDs subtypes e6 = tD

        go' :: [FD] -> Int -> [FD]
        go' $fieldSignature i
          = $returnStatement
                where $where1_pre = ${range.map(i ⇒ s"deleteAt i f$i").mkString("(", ", ", ")")}

delete$n' :: Int -> IO ()
delete$n' = delete$n 0
""")
      }

      out.close()
    }
  }

  /**
   * containers are boxed via their box methods
   */
  private def BoxedDataConstructor(f : Field) : String = {
    if (f.getType.isInstanceOf[ContainerType]) s"c'${name(f.getDeclaredIn)}_${name(f)}"
    else BoxedDataConstructor(f.getType)
  }

  private def isReference(f : Field) = f.getType().isInstanceOf[UserType] || "annotation".equals(f.getType.getSkillName)
}