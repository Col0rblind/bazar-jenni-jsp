<%@ include file="_top.jspf" %>
<%
    Fila b = (Fila) request.getAttribute("b");
    List<Fila> items = U.a(request, "items"), productos = U.a(request, "productos");
    int ranuras = (Integer) request.getAttribute("ranuras");
    String base = U.opts(productos, "id", "nombre", null, "— Producto —");   // se arma una vez y se marca la opción elegida
%>
<h2><%= b == null ? "Nuevo combo" : "Editar combo" %></h2>
<form method="post" action="<%= cp %>/combos" class="box form">
  <input type="hidden" name="accion" value="guardar">
  <% if (b != null) { %><input type="hidden" name="id" value="<%= b.i("id") %>"><% } %>
  <label>Nombre<input name="nombre" required value="<%= U.v(b, "nombre") %>" placeholder="Ej. 5 hojas, Kit escolar"></label>
  <label>Precio del combo<input type="number" name="precio" min="0.01" step="0.01" required value="<%= U.v(b, "precio") %>"></label>
  <h3>Contiene (producto y cantidad)</h3>
<% for (int i = 0; i < ranuras; i++) {
       Fila it = i < items.size() ? items.get(i) : null;
       String opc = it == null ? base : base.replace("value=\"" + it.i("producto_id") + "\"", "value=\"" + it.i("producto_id") + "\" selected"); %>
  <div class="ln2">
    <select name="prod" aria-label="Producto <%= i + 1 %>"><%= opc %></select>
    <input type="number" name="cant" min="1" step="1" value="<%= it == null ? 1 : it.i("cantidad") %>" aria-label="Cantidad <%= i + 1 %>">
  </div>
<% } %>
  <p class="m">Deja en blanco las filas que no uses.</p>
  <button class="pri">Guardar combo</button>
  <a class="btn" href="<%= cp %>/inventario">Cancelar</a>
</form>
<%@ include file="_bottom.jspf" %>
