package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Productos (alta, edicion, baja). Muestra tambien compras y combos. */
@WebServlet("/inventario")
public class InventarioServlet extends Base {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        try (Connection c = Db.con()) {
            List<Fila> provs = Db.q(c, "SELECT id, nombre FROM proveedor ORDER BY nombre");
            rq.setAttribute("proveedores", provs);

            String ed = p(rq, "editar");
            if (!ed.isEmpty()) {                                           // pantalla de edicion
                Fila prod = Db.uno(c, "SELECT * FROM producto WHERE id = ?", U.lng(ed));
                if (prod == null) { ir(rq, rs, "/inventario"); return; }
                rq.setAttribute("p", prod);
                rq.setAttribute("compras", Stock.comprasAbiertas(c, prod.get("compra_id") == null ? null : prod.i("compra_id")));
                ver(rq, rs, "inventario", "Editar producto", "producto_form");
                return;
            }

            String f = p(rq, "f"), sql = "SELECT t.*, pr.nombre AS proveedor, c.fecha AS cfecha, c.flete AS cflete FROM (" + Stock.PRODUCTOS
                + ") t LEFT JOIN proveedor pr ON pr.id = t.proveedor_id LEFT JOIN compra c ON c.id = t.compra_id";
            List<Object> a = new ArrayList<>();
            if (!f.isEmpty()) { sql += " WHERE t.nombre LIKE ? OR t.codigo LIKE ?"; a.add("%" + f + "%"); a.add("%" + f + "%"); }
            rq.setAttribute("productos", Db.q(c, sql + " ORDER BY t.nombre LIMIT 300", a.toArray()));
            rq.setAttribute("compras", Stock.comprasAbiertas(c, null));
            rq.setAttribute("todasCompras", Db.q(c, "SELECT c.*, pr.nombre AS proveedor, "
                + "(SELECT COUNT(*) FROM producto p WHERE p.compra_id = c.id) AS n, "
                + "(SELECT IFNULL(SUM(p.costo),0) FROM producto p WHERE p.compra_id = c.id) AS costo_art "
                + "FROM compra c LEFT JOIN proveedor pr ON pr.id = c.proveedor_id "
                + "ORDER BY (n >= c.num_articulos), c.fecha DESC, c.id DESC"));
            rq.setAttribute("combos", Stock.combos(c));
            rq.setAttribute("f", f);
        }
        ver(rq, rs, "inventario", "Inventario", "inventario");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        String accion = p(rq, "accion");
        long id = U.lng(p(rq, "id"));
        String volver = accion.equals("guardar") ? "/inventario?editar=" + id : "/inventario";

        if (accion.equals("eliminar")) {
            try (Connection c = Db.con()) { Db.up(c, "DELETE FROM producto WHERE id = ?", id); }
            ok(rq, "Producto eliminado");
            return "/inventario";
        }
        String nombre = p(rq, "nombre");
        if (nombre.isEmpty()) throw new Aviso("Escribe el nombre del producto", volver);
        String codigo = p(rq, "codigo");
        double precio = Math.max(0, U.num(p(rq, "precio"))), costo = Math.max(0, U.num(p(rq, "costo")));
        int stock = Math.max(0, U.ent(p(rq, "stock")));
        Integer prov = U.idOrNull(p(rq, "proveedor")), compra = U.idOrNull(p(rq, "compra"));

        try (Connection c = Db.con()) {
            String extra = "";
            if (compra != null) {                                           // el proveedor lo manda la compra
                Fila k = Db.uno(c, "SELECT proveedor_id FROM compra WHERE id = ?", compra);
                if (k == null) compra = null;
                else if (k.get("proveedor_id") != null && !Integer.valueOf(k.i("proveedor_id")).equals(prov)) {
                    prov = k.i("proveedor_id");
                    extra = " (el proveedor se tomó de la compra)";
                }
            }
            if (accion.equals("agregar")) {
                Db.ins(c, "INSERT INTO producto (nombre, codigo, precio, costo, stock, stock_inicial, proveedor_id, compra_id) VALUES (?,?,?,?,?,?,?,?)",
                       nombre, codigo.isEmpty() ? null : codigo, precio, costo, stock, stock, prov, compra);
                if (compra != null) {
                    Fila k = Db.uno(c, "SELECT (SELECT COUNT(*) FROM producto WHERE compra_id = c.id) n, num_articulos FROM compra c WHERE id = ?", compra);
                    if (k != null && k.i("n") >= k.i("num_articulos")) extra += " · Compra completa: pasó al historial";
                }
                ok(rq, "Producto \"" + nombre + "\" agregado" + extra);
            } else if (accion.equals("guardar")) {
                Fila old = Db.uno(c, "SELECT stock, stock_inicial FROM producto WHERE id = ?", id);
                if (old == null) throw new Aviso("El producto ya no existe", "/inventario");
                int inicial = old.i("stock_inicial") + Math.max(0, stock - old.i("stock"));   // subir stock = mas piezas compradas
                Db.up(c, "UPDATE producto SET nombre=?, codigo=?, precio=?, costo=?, stock=?, stock_inicial=?, proveedor_id=?, compra_id=? WHERE id=?",
                      nombre, codigo.isEmpty() ? null : codigo, precio, costo, stock, inicial, prov, compra, id);
                ok(rq, "Cambios guardados" + extra);
                return "/inventario";
            }
        }
        return "/inventario";
    }
}
