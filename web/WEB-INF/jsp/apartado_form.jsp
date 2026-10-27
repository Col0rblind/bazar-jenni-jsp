<%@ include file="_top.jspf" %>
<%
    Fila a = (Fila) request.getAttribute("a");
    List<Fila> lineas = U.a(request, "lineas"), clientes = U.a(request, "clientes");
%>
<h2>Editar apartado #<%= a.i("id") %></h2>
<form method="post" action="<%= cp %>/apartados" class="box form">
  <input type="hidden" name="accion" value="guardar"><input type="hidden" name="id" value="<%= a.i("id") %>">
  <label>Cliente<select name="cliente" required><%= U.opts(clientes, "id", "nombre", a.get("cliente_id"), "—") %></select></label>
  <label>Total abonado<input type="number" name="abonado" min="0" step="0.01" value="<%= a.s("abonado") %>"></label>
  <h3>Productos</h3>
  <div class="edl m"><span>Producto</span><span>Cantidad (0 = quitar)</span><span>Precio</span></div>
<% for (Fila l : lineas) { %>
  <div class="edl">
    <span><%= l.h("nombre") %><input type="hidden" name="lid" value="<%= l.i("id") %>"></span>
    <input type="number" name="cant" min="0" step="1" value="<%= l.i("cantidad") %>" aria-label="Cantidad de <%= l.h("nombre") %>">
    <input type="number" name="precio" min="0" step="0.01" value="<%= l.s("precio") %>" aria-label="Precio de <%= l.h("nombre") %>">
  </div>
<% } %>
  <p class="m">El stock y la caja se ajustan solos al guardar.</p>
  <button class="pri">Guardar cambios</button>
  <a class="btn" href="<%= cp %>/apartados">Cancelar</a>
</form>
<%@ include file="_bottom.jspf" %>
