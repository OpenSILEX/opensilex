@echo off

setlocal

cd /d %~dp0

java -jar opensilex.jar %*
