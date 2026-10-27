package bazar;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Utilidades de formato, conversión y HTML. */
public final class U {
    private U() {}

    public static final String[][] TEMAS = {
        {"light", "Claro"}, {"dark", "Oscuro"}, {"sakura", "Sakura 🌸"}, {"navidad", "Navideño 🎄"},
        {"halloween", "Halloween 🎃"}, {"muertos", "Día de muertos 💀"}, {"valentin", "San Valentín ❤️"},
        {"mexico", "Independencia de México"}, {"reyes", "Reyes Magos 👑"}};

    private static final DateTimeFormatter FH = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final DateTimeFormatter FD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @SuppressWarnings("unchecked")
    public static <T> T a(HttpServletRequest rq, String nombre) { return (T) rq.getAttribute(nombre); }

    /** Escapa texto para HTML (contenido y atributos). */
    public static String e(Object o) {
        if (o == null) return "";
        String s = o.toString();
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                case '\'': b.append("&#39;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /** Valor escapado de una fila que puede ser null (formularios nuevos). */
    public static String v(Fila f, String k) { return f == null ? "" : f.h(k); }

    public static String url(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    public static String money(double n) { return String.format(Locale.US, "$%,.2f", n); }
    public static double r2(double d) { return Math.round(d * 100.0) / 100.0; }

    public static double num(String s) {
        if (s == null) return 0;
        try {
            double d = Double.parseDouble(s.trim().replace(',', '.'));
            return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
        } catch (NumberFormatException x) { return 0; }
    }
    public static int ent(String s) { return (int) Math.floor(num(s)); }
    public static long lng(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception x) { return 0; }
    }
    public static Integer idOrNull(String s) { long v = lng(s); return v > 0 ? Integer.valueOf((int) v) : null; }

    public static Timestamp ahora() { return new Timestamp(System.currentTimeMillis()); }
    public static Date hoy() { return new Date(System.currentTimeMillis()); }
    public static String fh(Timestamp t) { return t.toLocalDateTime().format(FH); }
    public static String fd(Date d) { return d.toLocalDate().format(FD); }

    /** Genera las &lt;option&gt; de un &lt;select&gt;. */
    public static String opts(List<Fila> filas, String val, String txt, Object sel, String vacio) {
        StringBuilder b = new StringBuilder();
        String s = sel == null ? "" : sel.toString();
        if (vacio != null) b.append("<option value=\"\">").append(e(vacio)).append("</option>");
        for (Fila r : filas) {
            String v = r.s(val);
            b.append("<option value=\"").append(e(v)).append('"').append(v.equals(s) ? " selected" : "")
             .append('>').append(e(r.s(txt))).append("</option>");
        }
        return b.toString();
    }

    /** Icono SVG en línea (funciona sin internet). */
    public static String icono(String n) {
        String d;
        switch (n) {
            case "caja":  d = "<path d=\"M3 4h2l2 10h11l2-8H6\"/><circle cx=\"9\" cy=\"19\" r=\"1.5\"/><circle cx=\"17\" cy=\"19\" r=\"1.5\"/>"; break;
            case "box":   d = "<path d=\"M3 7l9-4 9 4v10l-9 4-9-4zM3 7l9 4 9-4M12 11v10\"/>"; break;
            case "truck": d = "<path d=\"M2 6h11v10H2zM13 10h4l3 3v3h-7\"/><circle cx=\"6\" cy=\"18\" r=\"1.5\"/><circle cx=\"17\" cy=\"18\" r=\"1.5\"/>"; break;
            case "users": d = "<circle cx=\"9\" cy=\"8\" r=\"3.5\"/><path d=\"M2 20c0-4 3-6 7-6s7 2 7 6M17 5a3.5 3.5 0 010 7M19 14c2 1 3 3 3 6\"/>"; break;
            case "clock": d = "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>"; break;
            case "chart": d = "<path d=\"M4 20V4M4 20h16M8 16v-5M13 16V8M18 16v-3\"/>"; break;
            case "cash":  d = "<rect x=\"2\" y=\"6\" width=\"20\" height=\"12\" rx=\"2\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/>"; break;
            default:      d = "";
        }
        return "<svg class=\"i\" viewBox=\"0 0 24 24\" aria-hidden=\"true\">" + d + "</svg>";
    }
}
