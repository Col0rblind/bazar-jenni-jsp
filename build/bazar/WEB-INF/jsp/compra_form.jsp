<%@ include file="_top.jspf" %>
<%
    Fila k = (Fila) request.getAttribute("k");
    List<Fila> provs = U.a(request, "proveedores");
%>
<h2><%= k == null ? "Nueva compra a proveedor" : "Editar compra" %></h2>
<form method="post" action="<%= cp %>/compras" class="box form">
  <input type="hidden" name="accion" value="guardar">
  <% if (k != null) { %><input type="hidden" name="id" value="<%= k.i("id") %>"><% } %>
  <div class="fg">
    <label>Fecha de la compra<input type="date" name="fecha" value="<%= k == null ? U.e(request.getAttribute("hoy")) : (k.get("fecha") == null ? "" : k.s("fecha")) %>"></label>
    <label>Proveedor<select name="proveedor"><%= U.opts(provs, "id", "nombre", k == null ? null : k.get("proveedor_id"), "—") %></select></label>
    <label>Flete (lo que cobró el proveedor)<input type="number" name="flete" min="0" step="0.01" value="<%= U.v(k, "flete") %>"></label>
    <label>¿Cuántos artículos llegaron en esta compra?<input type="number" name="articulos" min="1" step="1" required value="<%= U.v(k, "num_articulos") %>"></label>
  </div>
  <button class="pri">Guardar compra</button>
  <a class="btn" href="<%= cp %>/inventario">Cancelar</a>
</form>
<p class="m">La compra se cierra sola cuando registres todos sus artículos. Al cambiar el proveedor, los artículos de la compra cambian también.</p>
<%@ include file="_bottom.jspf" %>
