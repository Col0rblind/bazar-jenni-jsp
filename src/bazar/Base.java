package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Base de los servlets. Cada servlet implementa:
 *  - get(): prepara datos y reenvia a un JSP (vista).
 *  - post(): procesa el formulario y devuelve a donde redirigir (patron Post/Redirect/Get).
 */
public abstract class Base extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected abstract void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException;

    protected abstract String post(HttpServletRequest rq) throws Aviso, SQLException;

    @Override
    protected final void doGet(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException {
        try { get(rq, rs); }
        catch (SQLException e) { throw new ServletException(e); }
    }

    @Override
    protected final void doPost(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException {
        String destino;
        try { destino = post(rq); }
        catch (Aviso a) {
            flash(rq, "err", a.getMessage());
            destino = a.destino != null ? a.destino : rq.getServletPath();
        } catch (SQLException e) { throw new ServletException(e); }
        Cookie ck = (Cookie) rq.getAttribute("cookie");          // cookie pendiente (tema)
        if (ck != null) rs.addCookie(ck);
        rs.sendRedirect(rq.getContextPath() + destino);
    }

    /** Texto del parametro, sin espacios en los extremos ("" si no viene). */
    protected static String p(HttpServletRequest rq, String nombre) {
        String v = rq.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    protected static void ok(HttpServletRequest rq, String mensaje) { flash(rq, "ok", mensaje); }

    private static void flash(HttpServletRequest rq, String tipo, String mensaje) {
        rq.getSession().setAttribute("flash", new String[] {tipo, mensaje});
    }

    protected static String tema(HttpServletRequest rq) {
        if (rq.getCookies() != null)
            for (Cookie c : rq.getCookies())
                if (c.getName().equals("tema"))
                    for (String[] t : U.TEMAS) if (t[0].equals(c.getValue())) return t[0];
        return "light";
    }

    /** Reenvia al JSP /WEB-INF/jsp/<jsp>.jsp (los JSP no son accesibles directamente). */
    protected void ver(HttpServletRequest rq, HttpServletResponse rs, String seccion, String titulo, String jsp)
            throws ServletException, IOException {
        HttpSession s = rq.getSession();
        String[] f = (String[]) s.getAttribute("flash");
        if (f != null) { rq.setAttribute("msgTipo", f[0]); rq.setAttribute("msg", f[1]); s.removeAttribute("flash"); }
        rq.setAttribute("seccion", seccion);
        rq.setAttribute("titulo", titulo);
        rq.setAttribute("tema", tema(rq));
        String qs = rq.getQueryString();
        rq.setAttribute("ruta", rq.getServletPath() + (qs == null ? "" : "?" + qs));
        rq.getRequestDispatcher("/WEB-INF/jsp/" + jsp + ".jsp").forward(rq, rs);
    }

    protected static void ir(HttpServletRequest rq, HttpServletResponse rs, String ruta) throws IOException {
        rs.sendRedirect(rq.getContextPath() + ruta);
    }
}
