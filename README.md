SKilL
=====

High performance, cross platform, cross language, easy-to-use serialization interface generator.

Documentation and Specification can be found at:
http://www2.informatik.uni-stuttgart.de/cgi-bin/NCSTRL/NCSTRL_view.pl?id=TR-2017-01


This repository contains implementations for (alphatical order):

Language|Supported Features|Testsuite|Libs|Notes
-------|------------------|---------|----|-----
Ada 2012 |auto, append, documented, escaped, lazy(only reflection)| yes |commonAda| incomplete resource management
C 99 |mainly core features| does not compile | - | SKilL TR13, not maintained
C++ 11 (cpp) |customs, documented, escaped, lazy(only reflection)| yes | commonC++ | 
Haskell | only basic features | yes | - | some basic tests fail, not maintained
Java 8 |auto, append, customs, documented, escaped, interfaces, lazy(only reflection)| yes | [commonJVM](https://github.com/skill-lang/jvmCommon), commonJava | some multi-state support
Scala 2.12 |auto, append, customs, documented, escaped, interfaces, lazy, views| yes | [commonJVM](https://github.com/skill-lang/jvmCommon), commonScala | most complete implementation


Utilities
=========

Viewer for graphs stored in binary files: skillView

Basic reachability-based garbage collector: [skillGC](https://github.com/skill-lang/skillGC)

Eclipse-based IDE for .skill-Specifications (beta): skillEd


Usage
=====

TBD
