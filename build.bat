@echo off
rem Compila Bazar y lo copia a webapps de Tomcat.   Uso: build.bat C:\apache-tomcat-10.1.x
rem Requiere un JDK (no solo JRE) y mysql-connector-j-*.jar dentro de la carpeta lib\
setlocal
cd /d "%~dp0"
set TOMCAT=%~1
if "%TOMCAT%"=="" set TOMCAT=%CATALINA_HOME%
if not exist "%TOMCAT%\lib\servlet-api.jar" (echo Indica la carpeta de Tomcat 10.1+ & exit /b 1)
where javac >nul 2>nul || (echo No encuentro javac: instala un JDK 17+ & exit /b 1)
if not exist lib\mysql-connector-j-*.jar (echo Falta lib\mysql-connector-j-*.jar - ver lib\LEEME.txt & exit /b 1)

if exist build rmdir /s /q build
mkdir build\bazar\WEB-INF\classes build\bazar\WEB-INF\lib
xcopy /e /i /q /y web build\bazar >nul
copy /y lib\*.jar build\bazar\WEB-INF\lib >nul
javac -encoding UTF-8 -cp "%TOMCAT%\lib\servlet-api.jar" -d build\bazar\WEB-INF\classes src\bazar\*.java || exit /b 1
pushd build\bazar & jar cf ..\bazar.war . & popd

if exist "%TOMCAT%\webapps\bazar" rmdir /s /q "%TOMCAT%\webapps\bazar"
xcopy /e /i /q /y build\bazar "%TOMCAT%\webapps\bazar" >nul
echo Listo. Arranca Tomcat (bin\startup.bat) y abre http://localhost:8080/bazar/

