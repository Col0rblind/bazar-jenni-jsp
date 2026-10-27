package bazar;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Reglas de inventario, costos, combos y renglones de venta/apartado. */
public final class Stock {
    private Stock() {}

    /**
     * Productos con su costo por pieza: (costo del articulo + parte del flete de su compra) / piezas iniciales.
     * El flete se reparte entre max(articulos ya registrados, articulos que llegaron).
     */
    public static final String PRODUCTOS =
        "SELECT p.*, (p.costo + IFNULL(c.flete / GREATEST(k.n, c.num_articulos), 0)) / GREATEST(p.stock_inicial, 1) AS costo_unit "
      + "FROM producto p LEFT JOIN compra c ON c.id = p.compra_id "
      + "LEFT JOIN (SELECT compra_id, COUNT(*) n FROM producto WHERE compra_id IS NOT NULL GROUP BY compra_id) k ON k.compra_id = p.compra_id ";

    // ---------- Combos ----------
    public static final class Combo {
        public int id, disp;
        public String nombre, contiene = "";
        public double precio;
        public final List<int[]> items = new ArrayList<>();   // {producto_id, cantidad}
    }

    public static List<Combo> combos(Connection c) throws SQLException {
        Map<Integer, Combo> m = new LinkedHashMap<>();
        for (Fila r : Db.q(c, "SELECT id, nombre, precio FROM combo ORDER BY nombre")) {
            Combo b = new Combo();
            b.id = r.i("id"); b.nombre = r.s("nombre"); b.precio = r.d("precio");
            m.put(b.id, b);
        }
        for (Fila r : Db.q(c, "SELECT i.combo_id, i.producto_id, i.cantidad, p.nombre, p.stock FROM combo_item i "
                + "JOIN producto p ON p.id = i.producto_id ORDER BY i.combo_id, p.nombre")) {
            Combo b = m.get(r.i("combo_id"));
            if (b == null) continue;
            int q = Math.max(1, r.i("cantidad")), d = r.i("stock") / q;
            b.disp = b.items.isEmpty() ? d : Math.min(b.disp, d);
            b.items.add(new int[] {r.i("producto_id"), q});
            b.contiene += (b.contiene.isEmpty() ? "" : ", ") + q + "× " + r.s("nombre");
        }
        return new ArrayList<>(m.values());
    }

    public static Map<Integer, Combo> combosPorId(Connection c) throws SQLException {
        Map<Integer, Combo> m = new HashMap<>();
        for (Combo b : combos(c)) m.put(b.id, b);
        return m;
    }

    // ---------- Carrito ----------
    private static int idDe(String clave) { return Integer.parseInt(clave.substring(2)); }

    public static Map<Integer, Fila> productosPor(Connection c, Collection<Integer> ids) throws SQLException {
        Map<Integer, Fila> m = new HashMap<>();
        if (ids.isEmpty()) return m;
        for (Fila p : Db.q(c, PRODUCTOS + "WHERE p.id IN (" + Db.in(ids.size()) + ")", ids.toArray()))
            m.put(p.i("id"), p);
        return m;
    }

    /** Renglones del carrito con precio y nombre actuales. Claves del carrito: "p:ID" o "b:ID". */
    public static List<Fila> lineasCarrito(Connection c, Map<String, Integer> cart) throws SQLException {
        List<Fila> out = new ArrayList<>();
        if (cart.isEmpty()) return out;
        Map<Integer, Combo> cb = combosPorId(c);
        Set<Integer> ids = new HashSet<>();
        for (String k : cart.keySet()) if (k.startsWith("p:")) ids.add(idDe(k));
        Map<Integer, Fila> pr = productosPor(c, ids);
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            String k = e.getKey();
            int id = idDe(k);
            Fila f = new Fila();
            f.put("k", k.substring(0, 1)); f.put("id", id); f.put("cantidad", e.getValue());
            if (k.startsWith("p:")) {
                Fila p = pr.get(id);
                if (p == null) continue;
                f.put("nombre", p.s("nombre")); f.put("precio", p.d("precio"));
            } else {
                Combo b = cb.get(id);
                if (b == null) continue;
                f.put("nombre", "Combo: " + b.nombre); f.put("precio", b.precio);
            }
            out.add(f);
        }
        return out;
    }

    public static double total(List<Fila> lineas) {
        double t = 0;
        for (Fila l : lineas) t += l.d("precio") * l.i("cantidad");
        return U.r2(t);
    }

    /** ¿Alcanza el stock para todo el carrito? (bloquear = FOR UPDATE dentro de una transaccion) */
    public static boolean cabe(Connection c, Map<String, Integer> cart, boolean bloquear) throws SQLException {
        Map<Integer, Integer> need = new HashMap<>();
        Map<Integer, Combo> cb = null;
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            int id = idDe(e.getKey()), q = e.getValue();
            if (e.getKey().startsWith("p:")) need.merge(id, q, Integer::sum);
            else {
                if (cb == null) cb = combosPorId(c);
                Combo b = cb.get(id);
                if (b != null) for (int[] it : b.items) need.merge(it[0], it[1] * q, Integer::sum);
            }
        }
        if (need.isEmpty()) return true;
        Map<Integer, Integer> stock = new HashMap<>();
        for (Fila r : Db.q(c, "SELECT id, stock FROM producto WHERE id IN (" + Db.in(need.size()) + ")" + (bloquear ? " FOR UPDATE" : ""), need.keySet().toArray()))
            stock.put(r.i("id"), r.i("stock"));
        for (Map.Entry<Integer, Integer> e : need.entrySet())
            if (stock.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    /** Guarda el carrito como renglones de una venta (col="venta_id") o apartado (col="apartado_id") y descuenta stock. */
    public static void volcar(Connection c, String col, long doc, Map<String, Integer> cart) throws SQLException {
        Map<Integer, Combo> cb = combosPorId(c);
        Set<Integer> ids = new HashSet<>();
        for (String k : cart.keySet()) {
            if (k.startsWith("p:")) ids.add(idDe(k));
            else { Combo b = cb.get(idDe(k)); if (b != null) for (int[] it : b.items) ids.add(it[0]); }
        }
        Map<Integer, Fila> pr = productosPor(c, ids);
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            int id = idDe(e.getKey()), q = e.getValue();
            String nombre; double precio, costo = 0; List<int[]> comp = new ArrayList<>();
            if (e.getKey().startsWith("p:")) {
                Fila p = pr.get(id);
                if (p == null) continue;
                nombre = p.s("nombre"); precio = p.d("precio"); costo = p.d("costo_unit");
                comp.add(new int[] {id, 1});
            } else {
                Combo b = cb.get(id);
                if (b == null) continue;
                nombre = "Combo: " + b.nombre; precio = b.precio; comp = b.items;
                for (int[] it : b.items) { Fila p = pr.get(it[0]); if (p != null) costo += p.d("costo_unit") * it[1]; }
            }
            long lid = Db.ins(c, "INSERT INTO linea (" + col + ", nombre, precio, cantidad, costo_unit) VALUES (?,?,?,?,?)",
                              doc, nombre, precio, q, costo);
            for (int[] it : comp) Db.up(c, "INSERT INTO linea_comp (linea_id, producto_id, cantidad) VALUES (?,?,?)", lid, it[0], it[1]);
            ajustar(c, lid, -q);
        }
    }

    // ---------- Stock de un renglon ----------
    /** Suma (o resta, si es negativo) piezas de un renglon al stock de los productos que contiene. */
    public static void ajustar(Connection c, long lineaId, int unidades) throws SQLException {
        Db.up(c, "UPDATE producto p JOIN linea_comp k ON k.producto_id = p.id SET p.stock = p.stock + ? * k.cantidad WHERE k.linea_id = ?",
              unidades, lineaId);
    }

    /** ¿Hay stock para sacar `extra` unidades mas de este renglon? */
    public static boolean hayStock(Connection c, long lineaId, int extra) throws SQLException {
        Fila r = Db.uno(c, "SELECT COUNT(*) n FROM linea_comp k JOIN producto p ON p.id = k.producto_id WHERE k.linea_id = ? AND p.stock < ? * k.cantidad",
                        lineaId, extra);
        return r.i("n") == 0;
    }

    /** Devuelve al stock todos los renglones de una venta/apartado. */
    public static void devolver(Connection c, String col, long doc) throws SQLException {
        for (Fila l : Db.q(c, "SELECT id, cantidad FROM linea WHERE " + col + " = ?", doc)) ajustar(c, l.l("id"), l.i("cantidad"));
    }

    /** Copia los renglones de un apartado a una venta (sin tocar el stock, ya estaba descontado). */
    public static void copiarAVenta(Connection c, long apartado, long venta) throws SQLException {
        for (Fila l : Db.q(c, "SELECT * FROM linea WHERE apartado_id = ? ORDER BY id", apartado)) {
            long n = Db.ins(c, "INSERT INTO linea (venta_id, nombre, precio, cantidad, costo_unit) VALUES (?,?,?,?,?)",
                            venta, l.s("nombre"), l.d("precio"), l.i("cantidad"), l.d("costo_unit"));
            Db.up(c, "INSERT INTO linea_comp (linea_id, producto_id, cantidad) SELECT ?, producto_id, cantidad FROM linea_comp WHERE linea_id = ?",
                  n, l.l("id"));
        }
    }

    /**
     * Aplica el formulario de edicion de renglones (campos lid[], cant[], precio[]):
     * ajusta stock, borra los renglones en 0 y devuelve el total nuevo.
     */
    public static double editarLineas(Connection c, String col, long doc, HttpServletRequest rq, String volver) throws SQLException, Aviso {
        String[] lid = rq.getParameterValues("lid"), cant = rq.getParameterValues("cant"), precio = rq.getParameterValues("precio");
        if (lid == null || cant == null || precio == null || lid.length != cant.length || lid.length != precio.length) lid = new String[0];
        Map<Long, Fila> actuales = new HashMap<>();
        for (Fila l : Db.q(c, "SELECT * FROM linea WHERE " + col + " = ?", doc)) actuales.put(l.l("id"), l);

        for (int k = 0; k < lid.length; k++) {                       // 1) validar stock
            Fila l = actuales.get(U.lng(lid[k]));
            if (l == null) continue;
            int d = Math.max(0, U.ent(cant[k])) - l.i("cantidad");
            if (d > 0 && !hayStock(c, l.l("id"), d)) throw new Aviso("No hay stock suficiente", volver);
        }
        for (int k = 0; k < lid.length; k++) {                       // 2) aplicar
            Fila l = actuales.get(U.lng(lid[k]));
            if (l == null) continue;
            int nq = Math.max(0, U.ent(cant[k]));
            ajustar(c, l.l("id"), l.i("cantidad") - nq);
            if (nq == 0) Db.up(c, "DELETE FROM linea WHERE id = ?", l.l("id"));
            else Db.up(c, "UPDATE linea SET cantidad = ?, precio = ? WHERE id = ?", nq, Math.max(0, U.num(precio[k])), l.l("id"));
        }
        return U.r2(Db.uno(c, "SELECT IFNULL(SUM(cantidad * precio), 0) t FROM linea WHERE " + col + " = ?", doc).d("t"));
    }

    // ---------- Compras e inventario ----------
    public static String etiquetaCompra(Fila k) {
        String p = k.s("proveedor").isEmpty() ? "Sin proveedor" : k.s("proveedor");
        return (k.get("fecha") == null ? "sin fecha" : k.fecha("fecha")) + " · " + p + " · flete " + k.m("flete")
             + " · " + k.i("n") + "/" + k.i("num_articulos") + " art.";
    }

    /** Compras que aun esperan articulos (mas la indicada, aunque ya este completa). */
    public static List<Fila> comprasAbiertas(Connection c, Integer incluir) throws SQLException {
        List<Fila> r = Db.q(c, "SELECT * FROM (SELECT c.*, pr.nombre AS proveedor, "
            + "(SELECT COUNT(*) FROM producto p WHERE p.compra_id = c.id) AS n "
            + "FROM compra c LEFT JOIN proveedor pr ON pr.id = c.proveedor_id) t "
            + "WHERE n < num_articulos OR id = ? ORDER BY fecha DESC, id DESC", incluir == null ? 0 : incluir);
        for (Fila k : r) k.put("etiqueta", etiquetaCompra(k));
        return r;
    }

    /** Totales de la mercancia registrada (costo, fletes, utilidad esperada). */
    public static Fila resumen(Connection c) throws SQLException {
        Fila r = Db.uno(c, "SELECT IFNULL(SUM(costo),0) cm, IFNULL(SUM(stock_inicial*precio),0) val, IFNULL(SUM(stock*precio),0) vs, "
            + "IFNULL(SUM(stock*costo_unit),0) cs FROM (" + PRODUCTOS + ") t");
        double fl = Db.uno(c, "SELECT IFNULL(SUM(flete),0) f FROM compra").d("f");
        r.put("fletes", fl);
        r.put("costo", r.d("cm") + fl);
        r.put("ut", r.d("val") - r.d("cm") - fl);
        return r;
    }
}
