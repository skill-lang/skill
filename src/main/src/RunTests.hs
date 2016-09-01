import Test.HUnit
import ZTest_unicodeReference
import ZTest_twoNodeBlocks
import ZTest_trivialType
import ZTest_noFieldRegressionTest
import ZTest_nodeFirstBlockOnly
import ZTest_fourColoredNodes
import ZTest_emptyBlocks
import ZTest_date
import ZTest_crossNodes
import ZTest_coloredNodes
import ZTest_ageUnrestricted
import ZTest_age

main = runTestTT $ TestList [  TestLabel "unicodeReference" ZTest_unicodeReference.run,
  TestLabel "twoNodeBlocks" ZTest_twoNodeBlocks.run,
  TestLabel "trivialType" ZTest_trivialType.run,
  TestLabel "noFieldRegressionTest" ZTest_noFieldRegressionTest.run,
  TestLabel "nodeFirstBlockOnly" ZTest_nodeFirstBlockOnly.run,
  TestLabel "fourColoredNodes" ZTest_fourColoredNodes.run,
  TestLabel "emptyBlocks" ZTest_emptyBlocks.run,
  TestLabel "date" ZTest_date.run,
  TestLabel "crossNodes" ZTest_crossNodes.run,
  TestLabel "coloredNodes" ZTest_coloredNodes.run,
  TestLabel "ageUnrestricted" ZTest_ageUnrestricted.run,
  TestLabel "age" ZTest_age.run  ]