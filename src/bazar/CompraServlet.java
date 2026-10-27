package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

/** Compras a proveedor (fecha, flete y numero de articulos). */
@WebServlet("/compras")
public class CompraServlet extends Base {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        String ed = p(rq, "editar");
        if (ed.isEmpty() && !p(rq, "nueva").equals("1")) { ir(rq, rs, "/inventario"); return; }
        try (Connection c = Db.con()) {
            Fila k = ed.isEmpty() ? null : Db.uno(c, "SELECT * FROM compra WHERE id = ?", U.lng(ed));
            if (!ed.isEmpty() && k == null) { ir(rq, rs, "/inventario"); return; }
            rq.setAttribute("k", k);
            rq.setAttribute("hoy", U.hoy().toString());
            rq.setAttribute("proveedores", Db.q(c, "SELECT id, nombre FROM proveedor ORDER BY nombre"));
        }
        ver(rq, rs, "inventario", ed.isEmpty() ? "Nueva compra" : "Editar compra", "compra_form");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        long id = U.lng(p(rq, "id"));
        try (Connection c = Db.con()) {
            if (p(rq, "accion").equals("eliminar")) {
                Db.up(c, "DELETE FROM compra WHERE id = ?", id);
                ok(rq, "Compra eliminada (los artículos se quedan, sin flete)");
                return "/inventario";
            }
            String volver = id > 0 ? "/compras?editar=" + id : "/compras?nueva=1";
            Date fecha = null;
            try { if (!p(rq, "fecha").isEmpty()) fecha = Date.valueOf(p(rq, "fecha")); }
            catch (IllegalArgumentException x) { throw new Aviso("Fecha no válida", volver); }
            int na = U.ent(p(rq, "articulos"));
            if (na < 1) throw new Aviso("Escribe cuántos artículos llegaron", volver);
            Integer prov = U.idOrNull(p(rq, "proveedor"));
            double flete = Math.max(0, U.num(p(rq, "flete")));

            if (id > 0) {
                Db.up(c, "UPDATE compra SET fecha=?, proveedor_id=?, flete=?, num_articulos=? WHERE id=?", fecha, prov, flete, na, id);
                Db.up(c, "UPDATE producto SET proveedor_id=? WHERE compra_id=?", prov, id);   // los artículos siguen al proveedor de la compra
            } else {
                Db.ins(c, "INSERT INTO compra (fecha, proveedor_id, flete, num_articulos) VALUES (?,?,?,?)", fecha, prov, flete, na);
            }
        }
        ok(rq, "Compra guardada");
        return "/inventario";
    }
}
