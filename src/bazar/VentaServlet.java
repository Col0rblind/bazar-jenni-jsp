package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Historial de ventas por periodo, utilidad, edicion/cancelacion y exportacion a CSV. */
@WebServlet("/ventas")
public class VentaServlet extends Base {
    private static final long serialVersionUID = 1L;

    private static final String PRODUCTOS_V = "(SELECT GROUP_CONCAT(CONCAT(l.cantidad, '× ', l.nombre) ORDER BY l.id SEPARATOR ', ') FROM linea l WHERE l.venta_id = v.id) AS productos";

    /** Periodo elegido: day | week (lun-dom) | month | all, alrededor de la fecha fd. */
    private static final class Periodo {
        final String per; final LocalDate fd; final Timestamp desde, hasta;
        Periodo(HttpServletRequest rq) {
            String x = p(rq, "per");
            per = x.equals("week") || x.equals("month") || x.equals("all") ? x : "day";
            LocalDate d;
            try { d = LocalDate.parse(p(rq, "fd")); } catch (Exception e) { d = LocalDate.now(); }
            fd = d;
            LocalDate a = null, b = null;
            if (per.equals("day")) { a = d; b = d.plusDays(1); }
            else if (per.equals("week")) { a = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); b = a.plusDays(7); }
            else if (per.equals("month")) { a = d.withDayOfMonth(1); b = a.plusMonths(1); }
            desde = a == null ? null : Timestamp.valueOf(a.atStartOfDay());
            hasta = b == null ? null : Timestamp.valueOf(b.atStartOfDay());
        }
        /** Condicion SQL sobre una columna de fecha ("" si es todo el historial). */
        String sql(String col) { return desde == null ? "" : " AND " + col + " >= ? AND " + col + " < ?"; }
        Object[] par(int veces) {
            if (desde == null) return new Object[0];
            Object[] o = new Object[2 * veces];
            for (int i = 0; i < veces; i++) { o[2 * i] = desde; o[2 * i + 1] = hasta; }
            return o;
        }
    }

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        Periodo pe = new Periodo(rq);
        try (Connection c = Db.con()) {
            String ed = p(rq, "editar");
            if (!ed.isEmpty()) {
                Fila v = Db.uno(c, "SELECT * FROM venta WHERE id = ? AND estado = 'ok'", U.lng(ed));
                if (v == null) { ir(rq, rs, "/ventas"); return; }
                rq.setAttribute("v", v);
                rq.setAttribute("lineas", Db.q(c, "SELECT * FROM linea WHERE venta_id = ? ORDER BY id", v.i("id")));
                rq.setAttribute("clientes", Db.q(c, "SELECT id, nombre FROM cliente ORDER BY nombre"));
                rq.setAttribute("fechaLocal", ((Timestamp) v.get("fecha")).toLocalDateTime().withNano(0).toString().substring(0, 16));
                ver(rq, rs, "ventas", "Editar venta #" + v.i("id"), "venta_form");
                return;
            }
            if (p(rq, "formato").equals("csv")) { csv(c, pe, rs); return; }

            List<Fila> ventas = Db.q(c, "SELECT v.*, cl.nombre AS cliente, " + PRODUCTOS_V
                + " FROM venta v LEFT JOIN cliente cl ON cl.id = v.cliente_id WHERE 1 = 1" + pe.sql("v.fecha") + " ORDER BY v.id DESC", pe.par(1));
            double total = 0; int ok = 0;
            for (Fila v : ventas) if (v.s("estado").equals("ok")) { total += v.d("total"); ok++; }
            Fila t = Db.uno(c, "SELECT IFNULL(SUM(l.cantidad), 0) piezas, IFNULL(SUM(l.cantidad * l.costo_unit), 0) costo "
                + "FROM linea l JOIN venta v ON v.id = l.venta_id WHERE v.estado = 'ok'" + pe.sql("v.fecha"), pe.par(1));

            rq.setAttribute("ventas", ventas);
            rq.setAttribute("total", total);
            rq.setAttribute("numOk", ok);
            rq.setAttribute("canceladas", ventas.size() - ok);
            rq.setAttribute("piezas", t.i("piezas"));
            rq.setAttribute("costo", t.d("costo"));
            rq.setAttribute("resumen", Stock.resumen(c));
            rq.setAttribute("apartados", Db.q(c, "SELECT a.*, cl.nombre AS cliente, "
                + "(SELECT GROUP_CONCAT(CONCAT(l.cantidad, '× ', l.nombre) ORDER BY l.id SEPARATOR ', ') FROM linea l WHERE l.apartado_id = a.id) AS productos "
                + "FROM apartado a LEFT JOIN cliente cl ON cl.id = a.cliente_id WHERE 1 = 1"
                + (pe.desde == null ? "" : " AND (a.fecha >= ? AND a.fecha < ? OR EXISTS (SELECT 1 FROM venta v WHERE v.apartado_id = a.id AND v.fecha >= ? AND v.fecha < ?))")
                + " ORDER BY a.id DESC", pe.par(2)));
            rq.setAttribute("abonos", Db.uno(c, "SELECT IFNULL(SUM(monto), 0) t FROM mov_caja WHERE tipo = 'abono'" + pe.sql("fecha"), pe.par(1)).d("t"));
            rq.setAttribute("per", pe.per);
            rq.setAttribute("fd", pe.fd.toString());
        }
        ver(rq, rs, "ventas", "Ventas", "ventas");
    }

    private void csv(Connection c, Periodo pe, HttpServletResponse rs) throws IOException, SQLException {
        List<Fila> filas = Db.q(c, "SELECT v.id, v.fecha, v.estado, v.apartado_id, l.nombre, l.cantidad, l.precio, l.costo_unit "
            + "FROM venta v JOIN linea l ON l.venta_id = v.id WHERE 1 = 1" + pe.sql("v.fecha") + " ORDER BY v.id, l.id", pe.par(1));
        rs.setContentType("text/csv; charset=UTF-8");
        rs.setHeader("Content-Disposition", "attachment; filename=\"ventas_" + pe.per + "_" + pe.fd + ".csv\"");
        PrintWriter w = rs.getWriter();
        w.print('\uFEFF');                                                  // para que Excel respete los acentos
        fila(w, "Folio", "Fecha", "Hora", "Producto", "Cantidad", "Precio", "Subtotal", "Costo unitario", "Costo total", "Utilidad", "Estado");
        double tv = 0, tc = 0;
        for (Fila f : filas) {
            Timestamp ts = (Timestamp) f.get("fecha");
            boolean ok = f.s("estado").equals("ok");
            int q = f.i("cantidad");
            double pr = f.d("precio"), co = ok ? f.d("costo_unit") : 0;
            if (ok) { tv += q * pr; tc += q * co; }
            fila(w, f.i("id"), ts.toLocalDateTime().toLocalDate(), ts.toLocalDateTime().toLocalTime().withNano(0), f.s("nombre"), q, pr, q * pr,
                 ok ? U.r2(co) : 0, ok ? U.r2(q * co) : 0, ok ? U.r2(q * (pr - co)) : 0,
                 !ok ? "Cancelada" : f.get("apartado_id") != null ? "Pagada (apartado #" + f.i("apartado_id") + ")" : "Pagada");
        }
        w.print("\r\n");
        fila(w, "", "", "", "TOTALES", "", "", U.r2(tv), "", U.r2(tc), U.r2(tv - tc), "");
        Fila r = Stock.resumen(c);
        fila(w, "", "", "", "Costo de la mercancía registrada (con fletes)", "", "", U.r2(r.d("costo")));
        fila(w, "", "", "", "Utilidad esperada al vender todo el stock", "", "", U.r2(r.d("ut")));
    }

    private static void fila(PrintWriter w, Object... celdas) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < celdas.length; i++)
            b.append(i > 0 ? "," : "").append('"').append(String.valueOf(celdas[i]).replace("\"", "\"\"")).append('"');
        w.print(b.append("\r\n"));
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        long id = U.lng(p(rq, "id"));
        String accion = p(rq, "accion"), volver = "/ventas?editar=" + id;
        String msg;
        if (accion.equals("cancelar")) {
            msg = Db.tx(c -> {
                Fila v = Db.uno(c, "SELECT * FROM venta WHERE id = ? FOR UPDATE", id);
                if (v == null || !v.s("estado").equals("ok")) throw new Aviso("La venta ya está cancelada");
                Stock.devolver(c, "venta_id", id);
                Db.up(c, "UPDATE venta SET estado = 'cancelada' WHERE id = ?", id);
                if (v.get("apartado_id") != null) Db.up(c, "UPDATE apartado SET estado = 'cancelado' WHERE id = ?", v.i("apartado_id"));
                Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'cancel', ?)", U.ahora(), -v.d("total"));
                return "Venta #" + id + " cancelada: piezas devueltas al stock";
            });
        } else {
            Integer cli = U.idOrNull(p(rq, "cliente"));
            Timestamp fecha;
            try { fecha = Timestamp.valueOf(LocalDateTime.parse(p(rq, "fecha"))); }
            catch (Exception x) { throw new Aviso("Fecha no válida", volver); }
            msg = Db.tx(c -> {
                Fila v = Db.uno(c, "SELECT * FROM venta WHERE id = ? AND estado = 'ok' FOR UPDATE", id);
                if (v == null) throw new Aviso("La venta ya no se puede editar", "/ventas");
                double total = Stock.editarLineas(c, "venta_id", id, rq, volver);
                Db.up(c, "UPDATE venta SET fecha=?, cliente_id=?, total=?, estado=? WHERE id=?", fecha, cli, total,
                      Db.uno(c, "SELECT COUNT(*) n FROM linea WHERE venta_id = ?", id).i("n") == 0 ? "cancelada" : "ok", id);
                if (v.get("apartado_id") == null && Math.abs(total - v.d("total")) > 0.001)
                    Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'ajuste', ?)", U.ahora(), total - v.d("total"));
                return "Venta #" + id + " actualizada";
            });
        }
        ok(rq, msg);
        return "/ventas";
    }
}
