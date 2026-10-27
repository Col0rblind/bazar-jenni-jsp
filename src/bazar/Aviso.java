package bazar;

/** Error de validación para mostrar al usuario (no es un fallo técnico). */
public class Aviso extends Exception {
    private static final long serialVersionUID = 1L;
    public final String destino;

    public Aviso(String mensaje) { this(mensaje, null); }

    public Aviso(String mensaje, String destino) {
        super(mensaje);
        this.destino = destino;
    }
}
