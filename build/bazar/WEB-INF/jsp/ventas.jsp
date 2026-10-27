<%@ include file="_top.jspf" %>
<%
    List<Fila> ventas = U.a(request, "ventas"), apartados = U.a(request, "apartados");
    Fila r = (Fila) request.getAttribute("resumen");
    double total = (Double) request.getAttribute("total"), costo = (Double) request.getAttribute("costo"), abonos = (Double) request.getAttribute("abonos");
    int canceladas = (Integer) request.getAttribute("canceladas");
    String per = (String) request.getAttribute("per"), fd = (String) request.getAttribute("fd");
    String[][] periodos = {{"day", "Día"}, {"week", "Semana (lun–dom)"}, {"month", "Mes"}, {"all", "Todo"}};
%>
<h2>Ventas</h2>
<form method="get" action="<%= cp %>/ventas" class="bar">
  <select name="per" aria-label="Periodo">
  <% for (String[] x : periodos) { %><option value="<%= x[0] %>"<%= x[0].equals(per) ? " selected" : "" %>><%= x[1] %></option><% } %>
  </select>
  <input type="date" name="fd" value="<%= fd %>" aria-label="Fecha">
  <button>Ver</button>
  <a class="btn pri" href="<%= cp %>/ventas?formato=csv&amp;per=<%= per %>&amp;fd=<%= fd %>">Exportar a Excel (CSV)</a>
</form>

<div class="box">
  <span class="big"><%= U.money(total) %></span>
  <span class="m">Venta · Costo <%= U.money(costo) %> · </span><b style="color:var(--ok)">Utilidad <%= U.money(total - costo) %></b>
  <span class="m">· <%= request.getAttribute("numOk") %> ventas · <%= request.getAttribute("piezas") %> piezas<%= canceladas > 0 ? " · " + canceladas + " canceladas" : "" %></span>
</div>

<div class="box">
  <b>Mercancía registrada</b>
  <div>Costo de la mercancía <%= U.money(r.d("cm")) %> + fletes <%= U.money(r.d("fletes")) %> = <b><%= U.money(r.d("costo")) %></b></div>
  <div>Precio × piezas de todos los artículos: <%= U.money(r.d("val")) %> · Utilidad esperada: <b style="color:var(--ok)"><%= U.money(r.d("ut")) %></b></div>
  <div class="m">Lo que queda en stock: costo <%= U.money(r.d("cs")) %> · valor de venta <%= U.money(r.d("vs")) %> · utilidad por vender <%= U.money(r.d("vs") - r.d("cs")) %></div>
</div>

<div class="wrap"><table>
  <tr><th>Folio</th><th>Fecha</th><th>Productos</th><th>Cliente</th><th>Total</th><th></th></tr>
<% for (Fila v : ventas) { boolean ok = v.s("estado").equals("ok"); %>
  <tr style="<%= ok ? "" : "opacity:.5;text-decoration:line-through" %>">
    <td><%= v.i("id") %></td>
    <td><%= v.f("fecha") %></td>
    <td><%= v.h("productos") %><%= v.get("apartado_id") == null ? "" : " <span class=\"tag w\">Apartado #" + v.i("apartado_id") + "</span>" %></td>
    <td><%= v.h("cliente") %></td>
    <td><%= v.m("total") %></td>
    <td style="white-space:nowrap">
    <% if (ok) { %>
      <a class="btn" href="<%= cp %>/ventas?editar=<%= v.i("id") %>">Editar</a>
      <form class="en" method="post" action="<%= cp %>/ventas">
        <input type="hidden" name="accion" value="cancelar"><input type="hidden" name="id" value="<%= v.i("id") %>">
        <button onclick="return confirm('¿Cancelar la venta #<%= v.i("id") %>? Se regresan las piezas al stock.')">Cancelar</button>
      </form>
    <% } else { %>Cancelada<% } %>
    </td>
  </tr>
<% } %>
<% if (ventas.isEmpty()) { %><tr><td colspan="6" class="m">Sin ventas en este periodo.</td></tr><% } %>
</table></div>

<h3>Apartados del periodo</h3>
<p class="m">Abonos cobrados en el periodo: <b><%= U.money(abonos) %></b>. Al entregar un apartado se registra arriba como venta.</p>
<div class="wrap"><table>
  <tr><th>#</th><th>Fecha</th><th>Cliente</th><th>Productos</th><th>Total</th><th>Abonado</th><th>Resta</th><th>Estado</th></tr>
<% for (Fila a : apartados) { String st = a.s("estado"); %>
  <tr>
    <td><%= a.i("id") %></td><td><%= a.f("fecha") %></td><td><%= a.h("cliente") %></td><td><%= a.h("productos") %></td>
    <td><%= a.m("total") %></td><td><%= a.m("abonado") %></td><td><%= U.money(a.d("total") - a.d("abonado")) %></td>
    <td><span class="tag<%= st.equals("abierto") ? " w" : st.equals("liquidado") ? "" : " no" %>"><%= st.equals("liquidado") ? "Entregado" : st %></span></td>
  </tr>
<% } %>
<% if (apartados.isEmpty()) { %><tr><td colspan="8" class="m">Sin apartados en este periodo</td></tr><% } %>
</table></div>
<%@ include file="_bottom.jspf" %>
