import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import javax.swing.*;
import java.awt.*;

public class MenuClientes extends JFrame {
    private AgentContainer contenedorAgentes;

    public static void main(String[] args) {
        // Arrancamos la interfaz directamente
        new MenuClientes();
    }

    public MenuClientes() {
        super("GENERADOR DE COMPRADORES");

        // 1. CONEXIÓN CON JADE (Lo que antes hacías por terminal)
        conectarAJade();

        // 2. DISEÑO DE LA VENTANA
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Campos
        JTextField campoNombre = crearCampo(panel, "Nombre Cliente (sin espacios):");
        JTextField campoLibro = crearCampo(panel, "Libro que busca:");
        JTextField campoDinero = crearCampo(panel, "Presupuesto Máximo:");

        // Botón Crear
        JButton btnCrear = new JButton("CREAR CLIENTE");
        btnCrear.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCrear.setBackground(new Color(60, 179, 113)); // Verde
        btnCrear.setForeground(Color.WHITE);

        btnCrear.addActionListener(e -> {
            String nombre = campoNombre.getText().trim();
            String libro = campoLibro.getText().trim();
            String dinero = campoDinero.getText().trim();

            if (!nombre.isEmpty() && !libro.isEmpty() && !dinero.isEmpty()) {
                crearAgente(nombre, libro, dinero);
                campoNombre.setText(""); // Limpiar nombre para el siguiente
            } else {
                JOptionPane.showMessageDialog(this, "Rellena todos los datos");
            }
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(btnCrear);

        getContentPane().add(panel);
        setSize(300, 300);
        setLocationRelativeTo(null); // Centrar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void conectarAJade() {
        try {
            // Obtenemos el entorno JADE
            Runtime rt = Runtime.instance();

            // Creamos un perfil para conectarnos al Main Container (donde está el Vendedor)
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.MAIN_PORT, "1099"); // Puerto por defecto de JADE

            // Creamos el contenedor (es como ejecutar jade.Boot -container)
            contenedorAgentes = rt.createAgentContainer(p);
            System.out.println("--- Conectado al sistema JADE correctamente ---");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error conectando a JADE.\n¿Está el Vendedor encendido?");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void crearAgente(String nombre, String libro, String dinero) {
        try {
            // Argumentos para el agente Comprador
            Object[] args = new Object[]{libro, dinero};

            // CREAR EL AGENTE "Comprador"
            AgentController agente = contenedorAgentes.createNewAgent(nombre, "Comprador", args);
            agente.start();

            System.out.println("Cliente " + nombre + " creado exitosamente.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear agente: " + nombre + "\n" + e.getMessage());
        }
    }

    // Auxiliar para diseño
    private JTextField crearCampo(JPanel p, String texto) {
        JLabel l = new JLabel(texto);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
        JTextField tf = new JTextField(15);
        p.add(tf);
        p.add(Box.createVerticalStrut(5));
        return tf;
    }
}