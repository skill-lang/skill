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

Using Tests
===========

In order to use JSON specified tests, which were a result of an Projekt-INF,
first execute 
```
sbt test
```
in the main "skill" project. After that, you may run the tests, which were generated into the testsuite projects in the
testsuite folder. Currently, only the Java and Scala project have such tests.

In order to run the Scala test, navigate into the testsuites/scala folder an run
```
sbt test
```

The same may be done for Java. Navigate into the folder testsuites/java and run 
```
ant junitreport
```

Interoperability tests may only be run after having executed the previous testsuites at least once. If this is the case,
you may run the interoperability tests from within the testsuites/interoperability folder with the command
```
sbt test
```