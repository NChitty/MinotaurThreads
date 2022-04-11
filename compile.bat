@echo OFF
mkdir \build
cd src
dir /s /B *.java > ..\sources.txt
javac -d ..\build @..\sources.txt
@DEL ..\sources.txt
cd ..\build
jar -cvfm Minotaur.jar ..\manifest.txt *.class
@echo ON
