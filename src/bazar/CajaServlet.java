package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Punto de venta: buscar/escanear, carrito en sesion, cobrar y apartar. */
@WebServlet("/caja")
public class CajaServlet extends Base {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> carrito(HttpServletRequest rq) {
        HttpSession s = rq.getSession();
        Map<String, Integer> c = (Map<String, Integer>) s.getAttribute("carrito");
        if (c == null) { c = new LinkedHashMap<>(); s.setAttribute("carrito", c); }
        return c;
    }

    private static String sinCeros(String s) { return s.trim().replaceFirst("^0+", ""); }

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        String q = p(rq, "q");
        HttpSession s = rq.getSession();
        try (Connection c = Db.con()) {
            Map<String, Integer> cart = carrito(rq);
            List<Fila> lineas = Stock.lineasCarrito(c, cart);
            Set<String> vivas = new HashSet<>();
            for (Fila l : lineas) vivas.add(l.s("k") + ":" + l.i("id"));
            cart.keySet().retainAll(vivas);                       // quita lo que ya no existe
            double total = Stock.total(lineas);

            String sql = "SELECT id, nombre, precio, stock FROM producto WHERE stock > 0";
            List<Object> a = new ArrayList<>();
            if (!q.isEmpty()) {
                sql += " AND (nombre LIKE ? OR TRIM(LEADING '0' FROM codigo) = ?)";
                a.add("%" + q + "%"); a.add(sinCeros(q));
            }
            List<Stock.Combo> combos = new ArrayList<>();
            for (Stock.Combo b : Stock.combos(c))
                if (b.disp > 0 && (q.isEmpty() || b.nombre.toLowerCase().contains(q.toLowerCase()))) combos.add(b);

            String recibido = s.getAttribute("recibido") == null ? "" : (String) s.getAttribute("recibido");
            double cambio = !recibido.isEmpty() && total > 0 && U.num(recibido) >= total ? U.num(recibido) - total : -1;

            rq.setAttribute("productos", Db.q(c, sql + " ORDER BY nombre LIMIT 150", a.toArray()));
            rq.setAttribute("combos", combos);
            rq.setAttribute("lineas", lineas);
            rq.setAttribute("total", total);
            rq.setAttribute("clientes", Db.q(c, "SELECT id, nombre FROM cliente ORDER BY nombre"));
            rq.setAttribute("cliente", s.getAttribute("cliente"));
            rq.setAttribute("recibido", recibido);
            rq.setAttribute("cambio", cambio);
            rq.setAttribute("q", q);
        }
        ver(rq, rs, "caja", "Caja", "caja");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        HttpSession s = rq.getSession();
        if (rq.getParameter("cliente") != null) s.setAttribute("cliente", p(rq, "cliente"));
        if (rq.getParameter("recibido") != null) s.setAttribute("recibido", p(rq, "recibido"));

        String q = p(rq, "q"), volver = "/caja" + (q.isEmpty() ? "" : "?q=" + U.url(q));
        String accion = p(rq, "accion");
        Map<String, Integer> cart = carrito(rq);

        if (accion.matches("(add|mas|menos):[pb]:\\d+")) {
            String clave = accion.substring(accion.indexOf(':') + 1);
            cambiar(cart, clave, accion.startsWith("menos") ? -1 : 1, volver);
        } else if (accion.equals("escanear")) {
            String cod = p(rq, "codigo");
            if (!cod.isEmpty()) {
                try (Connection c = Db.con()) {
                    Fila prod = Db.uno(c, "SELECT id FROM producto WHERE TRIM(LEADING '0' FROM codigo) = ? AND stock > 0", sinCeros(cod));
                    if (prod == null) throw new Aviso("No encontrado o agotado: " + cod, volver);
                    cambiar(cart, "p:" + prod.i("id"), 1, volver);
                }
            }
        } else if (accion.equals("vaciar")) {
            cart.clear();
        } else if (accion.equals("cobrar")) {
            cobrar(rq, cart, volver);
            return "/caja";
        } else if (accion.equals("apartar")) {
            apartar(rq, cart, volver);
            return "/caja";
        }
        return volver;
    }

    private static void cambiar(Map<String, Integer> cart, String clave, int d, String volver) throws Aviso, SQLException {
        int nuevo = cart.getOrDefault(clave, 0) + d;
        if (nuevo < 1) { cart.remove(clave); return; }
        Integer antes = cart.put(clave, nuevo);
        if (d > 0) {
            try (Connection c = Db.con()) {
                if (!Stock.cabe(c, cart, false)) {
                    if (antes == null) cart.remove(clave); else cart.put(clave, antes);
                    throw new Aviso("No hay más stock", volver);
                }
            }
        }
    }

    private void cobrar(HttpServletRequest rq, Map<String, Integer> cart, String volver) throws Aviso, SQLException {
        HttpSession s = rq.getSession();
        String cli = (String) s.getAttribute("cliente"), rec = (String) s.getAttribute("recibido");
        String msg = Db.tx(c -> {
            List<Fila> lineas = Stock.lineasCarrito(c, cart);
            if (lineas.isEmpty()) throw new Aviso("La venta está vacía", volver);
            double total = Stock.total(lineas), recibido = rec == null || rec.isEmpty() ? total : U.num(rec);
            if (recibido < total) throw new Aviso("Lo recibido es menor al total", volver);
            if (!Stock.cabe(c, cart, true)) throw new Aviso("No hay stock suficiente", volver);
            long id = Db.ins(c, "INSERT INTO venta (fecha, cliente_id, total, estado) VALUES (?,?,?, 'ok')", U.ahora(), U.idOrNull(cli == null ? "" : cli), total);
            Stock.volcar(c, "venta_id", id, cart);
            Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'venta', ?)", U.ahora(), total);
            return "Venta #" + id + " registrada" + (recibido > total ? " · Cambio " + U.money(recibido - total) : "");
        });
        cart.clear(); s.removeAttribute("cliente"); s.removeAttribute("recibido");
        ok(rq, msg);
    }

    private void apartar(HttpServletRequest rq, Map<String, Integer> cart, String volver) throws Aviso, SQLException {
        HttpSession s = rq.getSession();
        String cli = (String) s.getAttribute("cliente"), rec = (String) s.getAttribute("recibido");
        Integer cliente = U.idOrNull(cli == null ? "" : cli);
        if (cliente == null) throw new Aviso("Elige un cliente para apartar", volver);
        String msg = Db.tx(c -> {
            List<Fila> lineas = Stock.lineasCarrito(c, cart);
            if (lineas.isEmpty()) throw new Aviso("La venta está vacía", volver);
            double total = Stock.total(lineas), anticipo = Math.max(0, U.num(rec));
            if (anticipo > total) throw new Aviso("El anticipo supera el total", volver);
            if (!Stock.cabe(c, cart, true)) throw new Aviso("No hay stock suficiente", volver);
            long id = Db.ins(c, "INSERT INTO apartado (fecha, cliente_id, total, abonado, estado) VALUES (?,?,?,?, 'abierto')", U.ahora(), cliente, total, anticipo);
            Stock.volcar(c, "apartado_id", id, cart);
            if (anticipo > 0) Db.up(c, "INSERT INTO mov_caja (fecha, tipo, monto) VALUES (?, 'abono', ?)", U.ahora(), anticipo);
            return "Apartado #" + id + " guardado";
        });
        cart.clear(); s.removeAttribute("cliente"); s.removeAttribute("recibido");
        ok(rq, msg);
    }
}
