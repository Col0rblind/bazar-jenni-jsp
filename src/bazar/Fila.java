package bazar;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;

/** Una fila de resultado JDBC con accesos cómodos para usarla desde los JSP. */
public class Fila extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public int i(String k)    { Object o = get(k); return o instanceof Number ? ((Number) o).intValue() : 0; }
    public long l(String k)   { Object o = get(k); return o instanceof Number ? ((Number) o).longValue() : 0; }
    public double d(String k) { Object o = get(k); return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    public String s(String k) { Object o = get(k); return o == null ? "" : o.toString(); }
    public String h(String k) { return U.e(s(k)); }          // texto escapado para HTML
    public String m(String k) { return U.money(d(k)); }      // $1,234.50
    public String f(String k) { Object o = get(k); return o instanceof Timestamp ? U.fh((Timestamp) o) : ""; }
    public String fecha(String k) { Object o = get(k); return o instanceof Date ? U.fd((Date) o) : "—"; }
}
