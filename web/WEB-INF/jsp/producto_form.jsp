<%@ include file="_top.jspf" %>
<h2>Editar producto</h2>
<form method="post" action="<%= cp %>/inventario" class="box">
  <input type="hidden" name="accion" value="guardar">
  <jsp:include page="_campos_producto.jsp" />
  <button class="pri">Guardar cambios</button>
  <a class="btn" href="<%= cp %>/inventario">Cancelar</a>
</form>
<p class="m">Si aumentas el stock se cuenta como piezas compradas (baja el costo por pieza); si lo reduces, no cambia el costo.</p>
<%@ include file="_bottom.jspf" %>
