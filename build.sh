#!/usr/bin/env bash
# Compila Bazar y lo copia a webapps de Tomcat.   Uso: ./build.sh /ruta/a/apache-tomcat-10.1.x
# Requiere un JDK (no solo JRE) y mysql-connector-j-*.jar dentro de la carpeta lib/
set -e
cd "$(dirname "$0")"
TOMCAT="${1:-$CATALINA_HOME}"
[ -f "$TOMCAT/lib/servlet-api.jar" ] || { echo "Indica la carpeta de Tomcat 10.1+ (debe existir lib/servlet-api.jar)"; exit 1; }
command -v javac >/dev/null || { echo "No encuentro javac: instala un JDK 17+"; exit 1; }
ls lib/mysql-connector-j-*.jar >/dev/null 2>&1 || { echo "Falta lib/mysql-connector-j-*.jar (ver lib/LEEME.txt)"; exit 1; }

rm -rf build && mkdir -p build/bazar/WEB-INF/classes build/bazar/WEB-INF/lib
cp -r web/. build/bazar/
cp lib/*.jar build/bazar/WEB-INF/lib/
javac -encoding UTF-8 -cp "$TOMCAT/lib/servlet-api.jar" -d build/bazar/WEB-INF/classes src/bazar/*.java
(cd build/bazar && jar cf ../bazar.war .)

rm -rf "$TOMCAT/webapps/bazar" "$TOMCAT/webapps/bazar.war"
cp -r build/bazar "$TOMCAT/webapps/bazar"
echo "Listo. Arranca Tomcat (bin/startup.sh) y abre http://localhost:8080/bazar/"
