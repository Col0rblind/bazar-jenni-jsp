package bazar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/** Proveedores y clientes (misma estructura, dos direcciones). */
@WebServlet({"/proveedores", "/clientes"})
public class TerceroServlet extends Base {
    private static final long serialVersionUID = 1L;

    /** La tabla sale de la URL y solo puede ser una de estas dos (nunca de un parametro). */
    private static String tabla(HttpServletRequest rq) { return rq.getServletPath().equals("/proveedores") ? "proveedor" : "cliente"; }

    @Override
    protected void get(HttpServletRequest rq, HttpServletResponse rs) throws ServletException, IOException, SQLException {
        String t = tabla(rq);
        try (Connection c = Db.con()) {
            rq.setAttribute("filas", Db.q(c, "SELECT * FROM " + t + " ORDER BY nombre"));
            String ed = p(rq, "editar");
            rq.setAttribute("ed", ed.isEmpty() ? null : Db.uno(c, "SELECT * FROM " + t + " WHERE id = ?", U.lng(ed)));
        }
        rq.setAttribute("singular", t.equals("proveedor") ? "proveedor" : "cliente");
        String plural = t.equals("proveedor") ? "Proveedores" : "Clientes";
        ver(rq, rs, t.equals("proveedor") ? "proveedores" : "clientes", plural, "terceros");
    }

    @Override
    protected String post(HttpServletRequest rq) throws Aviso, SQLException {
        String t = tabla(rq), accion = p(rq, "accion"), ruta = rq.getServletPath();
        long id = U.lng(p(rq, "id"));
        try (Connection c = Db.con()) {
            if (accion.equals("eliminar")) {
                Db.up(c, "DELETE FROM " + t + " WHERE id = ?", id);
                ok(rq, "Eliminado");
                return ruta;
            }
            String nombre = p(rq, "nombre");
            if (nombre.isEmpty()) throw new Aviso("Escribe el nombre", id > 0 ? ruta + "?editar=" + id : ruta);
            if (accion.equals("guardar"))
                Db.up(c, "UPDATE " + t + " SET nombre=?, telefono=?, notas=? WHERE id=?", nombre, p(rq, "telefono"), p(rq, "notas"), id);
            else
                Db.ins(c, "INSERT INTO " + t + " (nombre, telefono, notas) VALUES (?,?,?)", nombre, p(rq, "telefono"), p(rq, "notas"));
        }
        ok(rq, "Guardado");
        return ruta;
    }
}
