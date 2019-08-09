SKilL
=====

Cross platform, cross language, easy-to-use serialization interface generator.

Documentation can be found at:
http://www2.informatik.uni-stuttgart.de/cgi-bin/NCSTRL/NCSTRL_view.pl?id=TR-2013-06&mod=0&engl=0&inst=FAK


This repository contains implementations for (alphatical order):

Language|Supported Features|Testsuite|Libs|Notes
-------|------------------|---------|----|-----
Ada 2012 |auto, append, documented, escaped, lazy(only reflection)| yes | [commonAda](https://github.com/skill-lang/adaCommon) | incomplete resource management
C 99 |mainly core features| does not compile | - | SKilL TR13, not maintained
C++ 11 (cpp) |auto, customs, documented, escaped, lazy(only reflection)| yes | [commonC++](https://github.com/skill-lang/cppCommon) | gcc and clang supported
C# (csharp) | like Java? | yes | [commonC#](https://github.com/skill-lang/csharpCommon) | dotnetcore?
Haskell | only basic features | yes | - | some basic tests fail, not maintained
Java 8 |auto, append, customs, documented, escaped, interfaces, lazy(only reflection)| yes | [commonJVM](https://github.com/skill-lang/jvmCommon), [commonJava](https://github.com/skill-lang/javaCommon) | some multi-state support
Scala 2.12 |auto, append, customs, documented, escaped, interfaces, lazy, views| yes | [commonJVM](https://github.com/skill-lang/jvmCommon), [commonScala](https://github.com/skill-lang/scalaCommon) | most complete implementation


Utilities
=========

Viewer for graphs stored in binary files: [skillView](https://github.com/skill-lang/skillView)

Basic reachability-based garbage collector: [skillGC](https://github.com/skill-lang/skillGC)

Eclipse-based IDE for .skill-Specifications (beta): skillEd


Usage
=====

TBD
=======
C++ 11 |documented,escaped,lazy(only reflection)| yes | commonC++ | 
Haskell | only basic features | yes | - | some basic tests fail
Java 8 |auto,append,customs,documented,escaped,interfaces,lazy(only reflection)| yes | commonJVM, commonJava | some multi-state support, last Arch.8 implementation
Scala 2.11 |auto,append,customs,documented,escaped,interfaces,lazy,views| yes | commonJVM, commonScala | most complete implementation
