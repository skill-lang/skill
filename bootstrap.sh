#! /usr/bin/env bash

cd `dirname .`
sbt assembly

rm -rv tmp; mkdir tmp
cd tmp
ln -s ../deps .
java -jar ../target/scala*/skill.jar ../sir/sir.skill -L scala --package de.ust.skill.sir
cd ..

rm -rv src/main/scala/de/ust/skill/sir
cp -rv tmp/de src/main/scala
cp -rv tmp/*.jar lib
rm -rv tmp

sbt assembly
cp target/scala*/skill.jar .
