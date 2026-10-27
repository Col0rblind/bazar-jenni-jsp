<%@ page isErrorPage="true" contentType="text/html; charset=UTF-8" import="bazar.U" %>
<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Error · Bazar</title><link rel="stylesheet" href="<%= request.getContextPath() %>/css/bazar.css"></head>
<body><main>
<h2>No se pudo completar la operación</h2>
<div class="box">
  <p>Revisa que <b>MySQL esté encendido</b>, que el usuario y la contraseña de <code>WEB-INF/db.properties</code> sean correctos
     y que el archivo <code>mysql-connector-j-*.jar</code> esté en <code>WEB-INF/lib</code>.</p>
  <p class="m"><%= exception == null ? "" : U.e(exception.toString()) %>
     <%= exception != null && exception.getCause() != null ? "<br>" + U.e(exception.getCause().toString()) : "" %></p>
  <a class="btn pri" href="<%= request.getContextPath() %>/caja">Volver a la Caja</a>
</div>
</main></body></html>
