@echo OFF
cd ..\src
javac -d ..\build Main.java
cd ..\build
jar -cvfm Minotaur.jar manifest.txt *.class
@echo ON
