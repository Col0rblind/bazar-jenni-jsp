<%@ include file="_top.jspf" %>
<%
    Fila ultimo = (Fila) request.getAttribute("ultimo");
    List<Fila> cortes = U.a(request, "cortes");
    double ventas = (Double) request.getAttribute("ventas"), abonos = (Double) request.getAttribute("abonos"),
           otros = (Double) request.getAttribute("otros"), esperado = (Double) request.getAttribute("esperado");
    Double dif = (Double) request.getAttribute("diferencia");
    String fondo = (String) request.getAttribute("fondo"), contado = (String) request.getAttribute("contado");
    Object idUltimo = ultimo == null ? null : ultimo.get("id");
%>
<h2>Corte de caja</h2>
<div class="box">
  <p class="m"><%= ultimo == null ? "Aún no hay cortes. Se cuenta todo lo registrado." : "Último corte: " + ultimo.f("fecha") %></p>
  <table>
    <tr><td>Ventas</td><td><%= U.money(ventas) %></td></tr>
    <tr><td>Abonos de apartados</td><td><%= U.money(abonos) %></td></tr>
    <tr><td>Devoluciones, cancelaciones y ajustes</td><td><%= U.money(otros) %></td></tr>
  </table>

  <%-- Calcular (GET): muestra lo que debe haber y la diferencia --%>
  <form method="get" action="<%= cp %>/corte" class="fg" style="margin-top:10px">
    <label>Fondo inicial en caja<input type="number" name="fondo" step="0.01" value="<%= U.e(fondo) %>" placeholder="0"></label>
    <label>Efectivo contado<input type="number" name="contado" step="0.01" value="<%= U.e(contado) %>" placeholder="0"></label>
    <label>&nbsp;<button>Calcular</button></label>
  </form>
  <p>Debe haber: <b class="big"><%= U.money(esperado) %></b>
     <% if (dif != null) { %> &nbsp; Diferencia: <b><%= U.money(dif) %></b><% } %></p>

  <%-- Hacer corte (POST) --%>
  <form method="post" action="<%= cp %>/corte">
    <input type="hidden" name="accion" value="corte">
    <input type="hidden" name="fondo" value="<%= U.e(fondo) %>"><input type="hidden" name="contado" value="<%= U.e(contado) %>">
    <button class="pri" onclick="return confirm('¿Cerrar la caja con este corte?')">Hacer corte</button>
  </form>
</div>

<h3>Cortes anteriores</h3>
<div class="wrap"><table>
  <tr><th>Fecha</th><th>Esperado</th><th>Contado</th><th>Diferencia</th><th>Nota</th><th></th></tr>
<% for (Fila k : cortes) { %>
  <tr>
    <td><%= k.f("fecha") %></td><td><%= k.m("esperado") %></td><td><%= k.m("contado") %></td><td><%= k.m("diferencia") %></td>
    <td><%= k.h("nota") %><%= k.get("modificado") == null ? "" : " <span class=\"tag w\">Editado</span>" %></td>
    <td style="white-space:nowrap">
      <a class="btn" href="<%= cp %>/corte?editar=<%= k.i("id") %>">Editar</a>
    <% if (k.get("id").equals(idUltimo)) { %>
      <form class="en" method="post" action="<%= cp %>/corte">
        <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<%= k.i("id") %>">
        <button onclick="return confirm('Eliminar el último corte reabre la caja: las ventas y abonos desde el corte anterior volverán a contarse. ¿Continuar?')">Eliminar</button>
      </form>
    <% } %>
    </td>
  </tr>
<% } %>
<% if (cortes.isEmpty()) { %><tr><td colspan="6" class="m">Sin cortes.</td></tr><% } %>
</table></div>
<%@ include file="_bottom.jspf" %>
