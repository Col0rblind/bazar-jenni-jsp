<%@ include file="_top.jspf" %>
<%
    List<Fila> productos = U.a(request, "productos"), lineas = U.a(request, "lineas"), clientes = U.a(request, "clientes");
    List<Stock.Combo> combos = U.a(request, "combos");
    double total = (Double) request.getAttribute("total"), cambio = (Double) request.getAttribute("cambio");
    String q = (String) request.getAttribute("q"), recibido = (String) request.getAttribute("recibido");
%>
<div class="pos">
  <section>
    <%-- Buscar (GET) --%>
    <form method="get" action="<%= cp %>/caja" class="bar">
      <input type="search" name="q" value="<%= U.e(q) %>" placeholder="Buscar producto por nombre o código" aria-label="Buscar producto">
      <button>Buscar</button>
      <% if (!q.isEmpty()) { %><a class="btn" href="<%= cp %>/caja">Limpiar</a><% } %>
    </form>

    <%-- Lector de código de barras: escribe el código y Enter (se envía con el formulario de la venta) --%>
    <div class="bar">
      <input type="text" name="codigo" form="fcaja" placeholder="Código de barras (lector) + Enter" autocomplete="off" aria-label="Código de barras">
      <button name="accion" value="escanear" form="fcaja">Agregar código</button>
    </div>

    <div class="grid">
    <% for (Stock.Combo b : combos) { %>
      <button class="card cb" form="fcaja" name="accion" value="add:b:<%= b.id %>">
        <b><%= U.e(b.nombre) %></b><span><%= U.money(b.precio) %></span><small>Combo · <%= b.disp %> disponibles</small>
      </button>
    <% } %>
    <% for (Fila p : productos) { %>
      <button class="card" form="fcaja" name="accion" value="add:p:<%= p.i("id") %>">
        <b><%= p.h("nombre") %></b><span><%= p.m("precio") %></span><small><%= p.i("stock") %> disponibles</small>
      </button>
    <% } %>
    </div>
    <% if (combos.isEmpty() && productos.isEmpty()) { %><p class="m">No hay productos disponibles. Agrégalos en Inventario.</p><% } %>
  </section>

  <aside>
    <h3>Venta actual</h3>
    <form id="fcaja" method="post" action="<%= cp %>/caja">
      <input type="hidden" name="q" value="<%= U.e(q) %>">
    <% for (Fila l : lineas) { String clave = l.s("k") + ":" + l.i("id"); %>
      <div class="ln">
        <span><%= l.h("nombre") %></span>
        <span>
          <button name="accion" value="menos:<%= clave %>" aria-label="Quitar una pieza">−</button>
          <%= l.i("cantidad") %>
          <button name="accion" value="mas:<%= clave %>" aria-label="Agregar una pieza">+</button>
        </span>
        <b><%= U.money(l.d("precio") * l.i("cantidad")) %></b>
      </div>
    <% } %>
    <% if (lineas.isEmpty()) { %><p class="m">Toca un producto para agregarlo.</p><% } %>

      <h2 class="big">Total <%= U.money(total) %></h2>
      <select name="cliente" aria-label="Cliente"><%= U.opts(clientes, "id", "nombre", request.getAttribute("cliente"), "Cliente (opcional)") %></select>
      <input type="number" name="recibido" min="0" step="0.01" placeholder="Recibido o anticipo" value="<%= U.e(recibido) %>" aria-label="Recibido o anticipo">
      <% if (cambio >= 0) { %><p class="m">Cambio: <%= U.money(cambio) %></p><% } %>
      <div class="row">
        <button class="pri" name="accion" value="cobrar" onclick="return confirm('¿Cobrar <%= U.money(total) %>?')">Cobrar</button>
        <button name="accion" value="apartar" onclick="return confirm('¿Apartar esta venta?')">Apartar</button>
        <button name="accion" value="vaciar" onclick="return confirm('¿Vaciar la venta actual?')">Vaciar</button>
      </div>
      <p class="m">Para ver el cambio, escribe lo recibido y presiona Enter.</p>
    </form>
  </aside>
</div>
<%@ include file="_bottom.jspf" %>
