package bazar;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

/** Al arrancar: lee db.properties y crea la base bazar_datos y sus tablas si no existen. */
@WebListener
public class Init implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent ev) {
        ServletContext ctx = ev.getServletContext();
        Properties p = new Properties();
        try (InputStream in = ctx.getResourceAsStream("/WEB-INF/db.properties")) {
            if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception x) {
            ctx.log("Bazar: no se pudo leer db.properties: " + x);
        }
        Db.init(p);

        try (InputStream in = ctx.getResourceAsStream("/WEB-INF/schema.sql");
             Connection c = Db.conServidor();
             Statement st = c.createStatement()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder limpio = new StringBuilder();
            for (String linea : sql.split("\n")) {
                int k = linea.indexOf("--");          // quita comentarios
                limpio.append(k >= 0 ? linea.substring(0, k) : linea).append('\n');
            }
            for (String s : limpio.toString().split(";")) if (!s.trim().isEmpty()) st.execute(s.trim());
            ctx.log("Bazar: base de datos lista.");
        } catch (Exception x) {
            ctx.log("Bazar: no se pudo preparar la base de datos (revisa WEB-INF/db.properties y que MySQL esté encendido): " + x, x);
        }
    }
}
