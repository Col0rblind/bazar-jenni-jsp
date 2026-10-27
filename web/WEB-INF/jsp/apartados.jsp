<%@ include file="_top.jspf" %>
<% List<Fila> apartados = U.a(request, "apartados"); %>
<h2>Apartados</h2>
<p class="m">Los apartados se crean desde la Caja con el botón Apartar (hace falta elegir un cliente).</p>
<div class="wrap"><table style="min-width:900px">
  <tr><th>#</th><th>Fecha</th><th>Cliente</th><th>Productos</th><th>Total</th><th>Abonado</th><th>Resta</th><th>Estado</th><th></th></tr>
<% for (Fila a : apartados) {
       String st = a.s("estado"); boolean abierto = st.equals("abierto"); %>
  <tr>
    <td><%= a.i("id") %></td>
    <td><%= a.f("fecha") %></td>
    <td><%= a.h("cliente") %></td>
    <td><%= a.h("productos") %></td>
    <td><%= a.m("total") %></td>
    <td><%= a.m("abonado") %></td>
    <td><%= U.money(a.d("total") - a.d("abonado")) %></td>
    <td><span class="tag<%= abierto ? " w" : st.equals("liquidado") ? "" : " no" %>"><%= st %></span></td>
    <td style="white-space:nowrap">
    <% if (abierto) { %>
      <form class="en" method="post" action="<%= cp %>/apartados">
        <input type="hidden" name="accion" value="abonar"><input type="hidden" name="id" value="<%= a.i("id") %>">
        <input type="number" name="monto" min="0.01" step="0.01" placeholder="Monto" style="width:90px" aria-label="Monto a abonar" required>
        <button>Abonar</button>
      </form>
      <a class="btn" href="<%= cp %>/apartados?editar=<%= a.i("id") %>">Editar</a>
      <% if (a.d("abonado") + 0.001 >= a.d("total")) { %>
      <form class="en" method="post" action="<%= cp %>/apartados">
        <input type="hidden" name="accion" value="entregar"><input type="hidden" name="id" value="<%= a.i("id") %>">
        <button class="pri" onclick="return confirm('¿Entregar el apartado y registrarlo como venta?')">Entregar</button>
      </form>
      <% } %>
      <form class="en" method="post" action="<%= cp %>/apartados">
        <input type="hidden" name="accion" value="cancelar"><input type="hidden" name="id" value="<%= a.i("id") %>">
        <button onclick="return confirm('¿Cancelar el apartado? Se regresan las piezas al stock y se devuelve lo abonado.')">Cancelar</button>
      </form>
    <% } %>
    </td>
  </tr>
<% } %>
<% if (apartados.isEmpty()) { %><tr><td colspan="9" class="m">Sin apartados.</td></tr><% } %>
</table></div>
<%@ include file="_bottom.jspf" %>
