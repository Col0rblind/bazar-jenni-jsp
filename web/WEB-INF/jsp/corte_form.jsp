<%@ include file="_top.jspf" %>
<% Fila k = (Fila) request.getAttribute("k"); %>
<h2>Editar corte del <%= k.f("fecha") %></h2>
<p class="msg err">Este corte ya está cerrado. Modificarlo cambia el historial de tu caja: hazlo solo para corregir un error.</p>
<form method="post" action="<%= cp %>/corte" class="box form">
  <input type="hidden" name="accion" value="guardar"><input type="hidden" name="id" value="<%= k.i("id") %>">
  <label>Fondo inicial<input type="number" name="fondo" step="0.01" value="<%= k.s("fondo") %>"></label>
  <label>Debe haber (esperado)<input type="number" name="esperado" step="0.01" value="<%= k.s("esperado") %>"></label>
  <label>Efectivo contado<input type="number" name="contado" step="0.01" value="<%= k.s("contado") %>"></label>
  <label>Nota (opcional)<input name="nota" maxlength="255" value="<%= k.h("nota") %>"></label>
  <p><button class="pri">Guardar cambios</button> <a class="btn" href="<%= cp %>/corte">Cancelar</a></p>
</form>
<%@ include file="_bottom.jspf" %>
