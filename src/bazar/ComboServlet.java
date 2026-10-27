package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Combos: paquetes con precio propio que descuentan varios productos. */
@WebServlet("/combos")
public class ComboServlet extends Base {
    private static final long serialVersionUID = 1L;
    static final int RANURAS = 8;

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        String ed = p(rq, "editar");
        if (ed.isEmpty() && !p(rq, "nuevo").equals("1")) { ir(rq, rs, "/inventario"); return; }
        try (Connection c = Db.con()) {
            Fila b = ed.isEmpty() ? null : Db.uno(c, "SELECT * FROM combo WHERE id = ?", U.lng(ed));
            if (!ed.isEmpty() && b == null) { ir(rq, rs, "/inventario"); return; }
            rq.setAttribute("b", b);
            rq.setAttribute("items", b == null ? new ArrayList<Fila>()
                : Db.q(c, "SELECT producto_id, cantidad FROM combo_item WHERE combo_id = ? ORDER BY producto_id", b.i("id")));
            rq.setAttribute("productos", Db.q(c, "SELECT id, nombre FROM producto ORDER BY nombre"));
            rq.setAttribute("ranuras", RANURAS);
        }
        ver(rq, rs, "inventario", ed.isEmpty() ? "Nuevo combo" : "Editar combo", "combo_form");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        long id = U.lng(p(rq, "id"));
        try (Connection c = Db.con()) {
            if (p(rq, "accion").equals("eliminar")) {
                Db.up(c, "DELETE FROM combo WHERE id = ?", id);
                ok(rq, "Combo eliminado (los productos no se borran)");
                return "/inventario";
            }
            String volver = id > 0 ? "/combos?editar=" + id : "/combos?nuevo=1";
            String nombre = p(rq, "nombre");
            double precio = U.num(p(rq, "precio"));
            Map<Integer, Integer> items = new LinkedHashMap<>();            // une productos repetidos
            String[] prod = rq.getParameterValues("prod"), cant = rq.getParameterValues("cant");
            for (int i = 0; prod != null && cant != null && i < Math.min(prod.length, cant.length); i++) {
                Integer pid = U.idOrNull(prod[i]);
                int q = U.ent(cant[i]);
                if (pid != null && q > 0) items.merge(pid, q, Integer::sum);
            }
            if (nombre.isEmpty() || precio <= 0 || items.isEmpty()) throw new Aviso("Falta el nombre, el precio o los productos", volver);

            c.setAutoCommit(false);
            try {
                if (id > 0) {
                    Db.up(c, "UPDATE combo SET nombre=?, precio=? WHERE id=?", nombre, precio, id);
                    Db.up(c, "DELETE FROM combo_item WHERE combo_id=?", id);
                } else id = Db.ins(c, "INSERT INTO combo (nombre, precio) VALUES (?,?)", nombre, precio);
                for (Map.Entry<Integer, Integer> e : items.entrySet())
                    Db.up(c, "INSERT INTO combo_item (combo_id, producto_id, cantidad) VALUES (?,?,?)", id, e.getKey(), e.getValue());
                c.commit();
            } catch (SQLException | RuntimeException x) { c.rollback(); throw x; }
        }
        ok(rq, "Combo guardado");
        return "/inventario";
    }
}
