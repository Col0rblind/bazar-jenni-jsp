package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/** Apartados: abonar, editar, entregar (se vuelve venta) o cancelar. */
@WebServlet("/apartados")
public class ApartadoServlet extends Base {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        try (Connection c = Db.con()) {
            String ed = p(rq, "editar");
            if (!ed.isEmpty()) {
                Fila a = Db.uno(c, "SELECT * FROM apartado WHERE id = ? AND estado = 'abierto'", U.lng(ed));
                if (a == null) { ir(rq, rs, "/apartados"); return; }
                rq.setAttribute("a", a);
                rq.setAttribute("lineas", Db.q(c, "SELECT * FROM linea WHERE apartado_id = ? ORDER BY id", a.i("id")));
                rq.setAttribute("clientes", Db.q(c, "SELECT id, nombre FROM cliente ORDER BY nombre"));
                ver(rq, rs, "apartados", "Editar apartado #" + a.i("id"), "apartado_form");
                return;
            }
            rq.setAttribute("apartados", Db.q(c, "SELECT a.*, cl.nombre AS cliente, "
                + "(SELECT GROUP_CONCAT(CONCAT(l.cantidad, '× ', l.nombre) ORDER BY l.id SEPARATOR ', ') FROM linea l WHERE l.apartado_id = a.id) AS productos "
                + "FROM apartado a LEFT JOIN cliente cl ON cl.id = a.cliente_id ORDER BY a.id DESC"));
        }
        ver(rq, rs, "apartados", "Apartados", "apartados");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        long id = U.lng(p(rq, "id"));
        String accion = p(rq, "accion"), msg = "";

        if (accion.equals("abonar")) {
            double v = U.num(p(rq, "monto"));
            msg = Db.tx(c -> {
                Fila a = Db.uno(c, "SELECT * FROM apartado WHERE id = ? AND estado = 'abierto' FOR UPDATE", id);
                if (a == null) throw new Aviso("El apartado ya no está abierto");
                if (!(v > 0) || v > a.d("total") - a.d("abonado") + 0.001) throw new Aviso("Monto no válido");
                Db.up(c, "UPDATE apartado SET abonado = abonado + ? WHERE id = ?", v, id);
                Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'abono', ?)", U.ahora(), v);
                return "Abono de " + U.money(v) + " registrado";
            });
        } else if (accion.equals("entregar")) {
            msg = Db.tx(c -> {
                Fila a = Db.uno(c, "SELECT * FROM apartado WHERE id = ? AND estado = 'abierto' FOR UPDATE", id);
                if (a == null) throw new Aviso("El apartado ya no está abierto");
                if (a.d("abonado") + 0.001 < a.d("total")) throw new Aviso("Falta liquidar el apartado");
                Db.up(c, "UPDATE apartado SET estado = 'liquidado' WHERE id = ?", id);
                long v = Db.ins(c, "INSERT INTO venta (fecha, cliente_id, total, estado, apartado_id) VALUES (?,?,?, 'ok', ?)",
                                U.ahora(), a.get("cliente_id"), a.d("total"), id);
                Stock.copiarAVenta(c, id, v);
                return "Apartado entregado y registrado como venta #" + v;
            });
        } else if (accion.equals("cancelar")) {
            msg = Db.tx(c -> {
                Fila a = Db.uno(c, "SELECT * FROM apartado WHERE id = ? AND estado = 'abierto' FOR UPDATE", id);
                if (a == null) throw new Aviso("El apartado ya no está abierto");
                Stock.devolver(c, "apartado_id", id);
                if (a.d("abonado") > 0) Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'cancel', ?)", U.ahora(), -a.d("abonado"));
                Db.up(c, "UPDATE apartado SET estado = 'cancelado' WHERE id = ?", id);
                return "Apartado cancelado: piezas devueltas al stock y " + U.money(a.d("abonado")) + " devueltos al cliente";
            });
        } else if (accion.equals("guardar")) {
            String volver = "/apartados?editar=" + id;
            Integer cli = U.idOrNull(p(rq, "cliente"));
            double pd = Math.max(0, U.num(p(rq, "abonado")));
            if (cli == null) throw new Aviso("Elige un cliente", volver);
            msg = Db.tx(c -> {
                Fila a = Db.uno(c, "SELECT * FROM apartado WHERE id = ? AND estado = 'abierto' FOR UPDATE", id);
                if (a == null) throw new Aviso("El apartado ya no está abierto", "/apartados");
                double total = Stock.editarLineas(c, "apartado_id", id, rq, volver);
                if (pd > total + 0.001) throw new Aviso("Lo abonado supera el total", volver);      // revierte todo
                Db.up(c, "UPDATE apartado SET cliente_id=?, total=?, abonado=? WHERE id=?", cli, total, pd, id);
                if (Math.abs(pd - a.d("abonado")) > 0.001)
                    Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'ajuste', ?)", U.ahora(), pd - a.d("abonado"));
                return "Apartado actualizado";
            });
            ok(rq, msg);
            return "/apartados";
        }
        if (!msg.isEmpty()) ok(rq, msg);
        return "/apartados";
    }
}
