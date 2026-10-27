package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Guarda el tema visual elegido en una cookie. */
@WebServlet("/tema")
public class TemaServlet extends Base {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException {
        ir(rq, rs, "/caja");
    }

    @Override
    protected String post(HttpServletRequest rq) {
        String t = p(rq, "tema");
        for (String[] x : U.TEMAS) {
            if (x[0].equals(t)) {
                Cookie c = new Cookie("tema", t);
                c.setMaxAge(60 * 60 * 24 * 365);
                c.setPath(rq.getContextPath().isEmpty() ? "/" : rq.getContextPath());
                rq.setAttribute("cookie", c);
            }
        }
        String volver = p(rq, "volver");
        return volver.startsWith("/") && !volver.startsWith("//") ? volver : "/caja";
    }
}
