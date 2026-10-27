package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** Corte de caja: esperado = fondo + movimientos desde el ultimo corte. */
@WebServlet("/corte")
public class CorteServlet extends Base {
    private static final long serialVersionUID = 1L;

    private static double suma(Connection c, Timestamp desde, String... tipos) throws SQLException {
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < tipos.length; i++) in.append(i == 0 ? "?" : ",?");
        Object[] a = new Object[tipos.length + (desde == null ? 0 : 1)];
        System.arraycopy(tipos, 0, a, 0, tipos.length);
        if (desde != null) a[tipos.length] = desde;
        return Db.uno(c, "SELECT IFNULL(SUM(monto), 0) t FROM mov_caja WHERE tipo IN (" + in + ")" + (desde == null ? "" : " AND fecha > ?"), a).d("t");
    }

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        try (Connection c = Db.con()) {
            String ed = p(rq, "editar");
            if (!ed.isEmpty()) {
                Fila k = Db.uno(c, "SELECT * FROM corte WHERE id = ?", U.lng(ed));
                if (k == null) { ir(rq, rs, "/corte"); return; }
                rq.setAttribute("k", k);
                ver(rq, rs, "corte", "Editar corte", "corte_form");
                return;
            }
            Fila ultimo = Db.uno(c, "SELECT * FROM corte ORDER BY id DESC LIMIT 1");
            Timestamp desde = ultimo == null ? null : (Timestamp) ultimo.get("fecha");
            double ventas = suma(c, desde, "venta"), abonos = suma(c, desde, "abono"), otros = suma(c, desde, "cancel", "ajuste");
            String fondo = p(rq, "fondo"), contado = p(rq, "contado");
            double esperado = U.num(fondo) + ventas + abonos + otros;
            rq.setAttribute("ultimo", ultimo);
            rq.setAttribute("ventas", ventas);
            rq.setAttribute("abonos", abonos);
            rq.setAttribute("otros", otros);
            rq.setAttribute("esperado", esperado);
            rq.setAttribute("fondo", fondo);
            rq.setAttribute("contado", contado);
            rq.setAttribute("diferencia", contado.isEmpty() ? null : (Double) (U.num(contado) - esperado));
            rq.setAttribute("cortes", Db.q(c, "SELECT * FROM corte ORDER BY id DESC"));
        }
        ver(rq, rs, "corte", "Corte de caja", "corte");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        String accion = p(rq, "accion");
        long id = U.lng(p(rq, "id"));
        if (accion.equals("corte")) {
            if (p(rq, "contado").isEmpty()) throw new Aviso("Escribe el efectivo contado");
            double fondo = U.num(p(rq, "fondo")), contado = U.num(p(rq, "contado"));
            ok(rq, Db.tx(c -> {
                Fila ultimo = Db.uno(c, "SELECT fecha FROM corte ORDER BY id DESC LIMIT 1 FOR UPDATE");
                Timestamp desde = ultimo == null ? null : (Timestamp) ultimo.get("fecha");
                Timestamp ahora = U.ahora();
                double esperado = fondo + suma(c, desde, "venta", "abono", "cancel", "ajuste");
                Db.up(c, "INSERT INTO corte (fecha, fondo, esperado, contado, diferencia) VALUES (?,?,?,?,?)", ahora, fondo, esperado, contado, contado - esperado);
                return "Corte guardado · diferencia " + U.money(contado - esperado);
            }));
        } else if (accion.equals("guardar")) {
            double e = U.num(p(rq, "esperado")), ct = U.num(p(rq, "contado"));
            try (Connection c = Db.con()) {
                Db.up(c, "UPDATE corte SET fondo=?, esperado=?, contado=?, diferencia=?, nota=?, modificado=? WHERE id=?",
                      U.num(p(rq, "fondo")), e, ct, ct - e, p(rq, "nota"), U.ahora(), id);
            }
            ok(rq, "Corte actualizado");
        } else if (accion.equals("eliminar")) {
            try (Connection c = Db.con()) {
                Fila ultimo = Db.uno(c, "SELECT id FROM corte ORDER BY id DESC LIMIT 1");
                if (ultimo == null || ultimo.l("id") != id) throw new Aviso("Solo se puede eliminar el último corte");
                Db.up(c, "DELETE FROM corte WHERE id = ?", id);
            }
            ok(rq, "Corte eliminado: la caja se reabrió");
        }
        return "/corte";
    }
}
