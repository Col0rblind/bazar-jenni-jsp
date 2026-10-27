<%@ page contentType="text/html; charset=UTF-8" import="java.util.*,bazar.*" %>
<%-- Campos compartidos por "agregar" (inventario.jsp) y "editar" (producto_form.jsp). p = null → producto nuevo --%>
<%
    Fila p = (Fila) request.getAttribute("p");
    List<Fila> provs = U.a(request, "proveedores"), comps = U.a(request, "compras");
    String cp = request.getContextPath();
    if (p != null) { %><input type="hidden" name="id" value="<%= p.i("id") %>"><% }
%>
<div class="fg">
  <label>Producto<input name="nombre" required value="<%= U.v(p, "nombre") %>"></label>
  <label>Código (opcional)<input name="codigo" value="<%= U.v(p, "codigo") %>" autocomplete="off"></label>
  <label>Precio de venta por pieza<input type="number" name="precio" min="0" step="0.01" value="<%= U.v(p, "precio") %>"></label>
  <label>Costo del artículo (total)<input type="number" name="costo" min="0" step="0.01" value="<%= U.v(p, "costo") %>"></label>
  <label>Stock<input type="number" name="stock" min="0" step="1" value="<%= U.v(p, "stock") %>"></label>
  <label>Proveedor<select name="proveedor"><%= U.opts(provs, "id", "nombre", p == null ? null : p.get("proveedor_id"), "—") %></select></label>
  <label>Compra / flete<select name="compra"><%= U.opts(comps, "id", "etiqueta", p == null ? null : p.get("compra_id"), "—") %></select></label>
</div>
<p class="m">Si eliges una compra, el proveedor del artículo será el de esa compra. <a href="<%= cp %>/compras?nueva=1">Registrar una compra nueva</a></p>
