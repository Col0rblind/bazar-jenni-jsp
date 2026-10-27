<%@ include file="_top.jspf" %>
<%-- Sirve para /proveedores y /clientes: el servlet indica cuál es (seccion y singular) --%>
<%
    List<Fila> filas = U.a(request, "filas");
    Fila ed = (Fila) request.getAttribute("ed");
    String singular = (String) request.getAttribute("singular"), ruta = cp + "/" + sec;
%>
<h2><%= U.e(request.getAttribute("titulo")) %></h2>
<form method="post" action="<%= ruta %>" class="box">
  <input type="hidden" name="accion" value="<%= ed == null ? "agregar" : "guardar" %>">
  <% if (ed != null) { %><input type="hidden" name="id" value="<%= ed.i("id") %>"><% } %>
  <div class="fg">
    <label>Nombre<input name="nombre" required value="<%= U.v(ed, "nombre") %>"></label>
    <label>Teléfono<input type="tel" name="telefono" value="<%= U.v(ed, "telefono") %>"></label>
    <label>Notas<input name="notas" value="<%= U.v(ed, "notas") %>"></label>
  </div>
  <button class="pri"><%= ed == null ? "Agregar " + singular : "Guardar cambios" %></button>
  <% if (ed != null) { %><a class="btn" href="<%= ruta %>">Cancelar</a><% } %>
</form>

<div class="wrap"><table>
  <tr><th>Nombre</th><th>Teléfono</th><th>Notas</th><th></th></tr>
<% for (Fila x : filas) { %>
  <tr>
    <td><%= x.h("nombre") %></td><td><%= x.h("telefono") %></td><td><%= x.h("notas") %></td>
    <td style="white-space:nowrap">
      <a class="btn" href="<%= ruta %>?editar=<%= x.i("id") %>">Editar</a>
      <form class="en" method="post" action="<%= ruta %>">
        <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<%= x.i("id") %>">
        <button onclick="return confirm('¿Eliminar?')">Eliminar</button>
      </form>
    </td>
  </tr>
<% } %>
<% if (filas.isEmpty()) { %><tr><td colspan="4" class="m">Todavía no hay registros.</td></tr><% } %>
</table></div>
<%@ include file="_bottom.jspf" %>
