<%@ include file="_top.jspf" %>
<%
    List<Fila> productos = U.a(request, "productos"), compras = U.a(request, "todasCompras");
    List<Stock.Combo> combos = U.a(request, "combos");
    String f = (String) request.getAttribute("f");
%>
<h2>Inventario</h2>

<h3>Agregar producto</h3>
<form method="post" action="<%= cp %>/inventario" class="box">
  <input type="hidden" name="accion" value="agregar">
  <jsp:include page="_campos_producto.jsp" />
  <button class="pri">Agregar</button>
</form>

<form method="get" action="<%= cp %>/inventario" class="bar">
  <input type="search" name="f" value="<%= U.e(f) %>" placeholder="Filtrar por nombre o código…" aria-label="Filtrar">
  <button>Filtrar</button>
</form>
<div class="wrap"><table style="min-width:1100px">
  <tr><th>Producto</th><th>Código</th><th>Precio</th><th>Costo artículo</th><th>Compra (flete)</th><th>Costo por pieza c/flete</th><th>Stock</th><th>Proveedor</th><th>Estado</th><th></th></tr>
<% for (Fila p : productos) { int sk = p.i("stock"); %>
  <tr>
    <td><%= p.h("nombre") %></td>
    <td><%= p.h("codigo") %></td>
    <td><%= p.m("precio") %></td>
    <td><%= p.m("costo") %></td>
    <td><%= p.get("compra_id") == null ? "—" : p.fecha("cfecha") + " · " + p.m("cflete") %></td>
    <td><%= U.money(p.d("costo_unit")) %></td>
    <td><%= sk %></td>
    <td><%= p.h("proveedor") %></td>
    <td><span class="tag<%= sk <= 0 ? " no" : sk <= 5 ? " w" : "" %>"><%= sk <= 0 ? "Agotado" : sk <= 5 ? "Pocas piezas" : "Disponible" %></span></td>
    <td style="white-space:nowrap">
      <a class="btn" href="<%= cp %>/inventario?editar=<%= p.i("id") %>">Editar</a>
      <form class="en" method="post" action="<%= cp %>/inventario">
        <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<%= p.i("id") %>">
        <button onclick="return confirm('¿Eliminar este producto?')">Eliminar</button>
      </form>
    </td>
  </tr>
<% } %>
<% if (productos.isEmpty()) { %><tr><td colspan="10" class="m">Sin productos.</td></tr><% } %>
</table></div>

<h3>Compras a proveedor</h3>
<p class="m">Registra cada compra con su fecha, su flete y cuántos artículos llegaron. Elígela al agregar esos artículos; cuando registras todos, la compra se cierra sola y ya no aparece en la lista.</p>
<p><a class="btn pri" href="<%= cp %>/compras?nueva=1">Nueva compra</a></p>
<div class="wrap"><table style="min-width:820px">
  <tr><th>Fecha</th><th>Proveedor</th><th>Flete</th><th>Artículos</th><th>Costo de artículos</th><th>Total (artículos + flete)</th><th>Estado</th><th></th></tr>
<% for (Fila c : compras) { boolean abierta = c.i("n") < c.i("num_articulos"); %>
  <tr>
    <td><%= c.fecha("fecha") %></td>
    <td><%= c.get("proveedor") == null ? "—" : c.h("proveedor") %></td>
    <td><%= c.m("flete") %></td>
    <td><%= c.i("n") %> de <%= c.i("num_articulos") %></td>
    <td><%= U.money(c.d("costo_art")) %></td>
    <td><b><%= U.money(c.d("costo_art") + c.d("flete")) %></b></td>
    <td><span class="tag<%= abierta ? " w" : "" %>"><%= abierta ? "Abierta" : "Completa" %></span></td>
    <td style="white-space:nowrap">
      <a class="btn" href="<%= cp %>/compras?editar=<%= c.i("id") %>">Editar</a>
      <form class="en" method="post" action="<%= cp %>/compras">
        <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<%= c.i("id") %>">
        <button onclick="return confirm('¿Eliminar esta compra? Los artículos se quedan, pero sin flete.')">Eliminar</button>
      </form>
    </td>
  </tr>
<% } %>
<% if (compras.isEmpty()) { %><tr><td colspan="8" class="m">Aún no hay compras. Registra una con "Nueva compra".</td></tr><% } %>
</table></div>

<h3>Combos</h3>
<p class="m">Un combo es un paquete con precio propio. Al venderlo se descuenta del stock cada producto que contiene.</p>
<p><a class="btn pri" href="<%= cp %>/combos?nuevo=1">Nuevo combo</a></p>
<div class="wrap"><table>
  <tr><th>Combo</th><th>Contiene</th><th>Precio</th><th>Disponibles</th><th></th></tr>
<% for (Stock.Combo b : combos) { %>
  <tr>
    <td><%= U.e(b.nombre) %></td>
    <td><%= U.e(b.contiene) %></td>
    <td><%= U.money(b.precio) %></td>
    <td><span class="tag<%= b.disp > 0 ? "" : " no" %>"><%= b.disp > 0 ? String.valueOf(b.disp) : "Agotado" %></span></td>
    <td style="white-space:nowrap">
      <a class="btn" href="<%= cp %>/combos?editar=<%= b.id %>">Editar</a>
      <form class="en" method="post" action="<%= cp %>/combos">
        <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<%= b.id %>">
        <button onclick="return confirm('¿Eliminar este combo? Los productos no se borran.')">Eliminar</button>
      </form>
    </td>
  </tr>
<% } %>
<% if (combos.isEmpty()) { %><tr><td colspan="5" class="m">Aún no hay combos. Crea uno con "Nuevo combo".</td></tr><% } %>
</table></div>
<%@ include file="_bottom.jspf" %>
