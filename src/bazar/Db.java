package bazar;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Acceso JDBC mínimo: conexión, consultas y transacciones. Sin pool (app ligera). */
public final class Db {
    private static String host = "localhost", port = "3306", nombre = "bazar_datos", user = "root", pass = "root";
    private static String params = "useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";

    private Db() {}

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { /* se informa al intentar conectar */ }
    }

    public static void init(Properties p) {
        host = p.getProperty("db.host", host).trim();
        port = p.getProperty("db.port", port).trim();
        nombre = p.getProperty("db.name", nombre).trim();
        user = p.getProperty("db.user", user).trim();
        pass = p.getProperty("db.password", pass);
        params = p.getProperty("db.params", params).trim();
    }

    private static String url(String base) { return "jdbc:mysql://" + host + ":" + port + "/" + base + "?" + params; }

    /** Conexión a la base bazar_datos. */
    public static Connection con() throws SQLException {
        Connection c = DriverManager.getConnection(url(nombre), user, pass);
        try (Statement s = c.createStatement()) { s.execute("SET SESSION group_concat_max_len = 65535"); }
        return c;
    }

    /** Conexión al servidor sin elegir base (para crearla). */
    public static Connection conServidor() throws SQLException {
        return DriverManager.getConnection(url(""), user, pass);
    }

    private static void bind(PreparedStatement ps, Object[] a) throws SQLException {
        for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
    }

    public static List<Fila> q(Connection c, String sql, Object... a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, a);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<Fila> out = new ArrayList<>();
                while (rs.next()) {
                    Fila f = new Fila();
                    for (int i = 1; i <= n; i++) {
                        int t = md.getColumnType(i);
                        Object v = t == Types.TIMESTAMP ? rs.getTimestamp(i) : t == Types.DATE ? rs.getDate(i) : rs.getObject(i);
                        f.put(md.getColumnLabel(i), v);
                    }
                    out.add(f);
                }
                return out;
            }
        }
    }

    public static Fila uno(Connection c, String sql, Object... a) throws SQLException {
        List<Fila> r = q(c, sql, a);
        return r.isEmpty() ? null : r.get(0);
    }

    public static int up(Connection c, String sql, Object... a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, a);
            return ps.executeUpdate();
        }
    }

    /** INSERT que devuelve el id generado. */
    public static long ins(Connection c, String sql, Object... a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, a);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getLong(1) : 0; }
        }
    }

    /** "?,?,?" para cláusulas IN. */
    public static String in(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(i == 0 ? "?" : ",?");
        return b.toString();
    }

    @FunctionalInterface
    public interface Tx<T> { T run(Connection c) throws Aviso, SQLException; }

    /** Ejecuta el bloque en una transacción: confirma si termina bien, revierte si falla. */
    public static <T> T tx(Tx<T> t) throws Aviso, SQLException {
        try (Connection c = con()) {
            c.setAutoCommit(false);
            try {
                T r = t.run(c);
                c.commit();
                return r;
            } catch (Aviso | SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
