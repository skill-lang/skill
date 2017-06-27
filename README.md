SKilL
=====

Cross platform, cross language, easy-to-use serialization interface generator.

Documentation can be found at:
http://www2.informatik.uni-stuttgart.de/cgi-bin/NCSTRL/NCSTRL_view.pl?id=TR-2013-06&mod=0&engl=0&inst=FAK


This repository contains implementations for (alphatical order):

Language|Supported Features|Testsuite|Libs|Notes
-------|------------------|---------|----|-----
Ada 2012 |auto,append,documented,escaped,lazy(only reflection)| yes |commonAda| incomplete resource management
C 99 |mainly core features| does not compile | - | SKilL TR13, not maintained
C++ 11 |documented,escaped,lazy(only reflection)| yes | commonC++ | 
Haskell | only basic features | yes | - | some basic tests fail
Java 8 |auto,append,customs,documented,escaped,interfaces,lazy(only reflection)| yes | commonJVM, commonJava | some multi-state support, last Arch.8 implementation
Scala 2.11 |auto,append,customs,documented,escaped,interfaces,lazy,views| yes | commonJVM, commonScala | most complete implementation
