import jade.core. Profile;
import jade.core. ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import javax.swing.*;
import java.awt.*;

public class MenuClientes extends JFrame {
    private AgentContainer contenedorAgentes;

    public static void main(String[] args) {
        SwingUtilities. invokeLater(() -> new MenuClientes());
    }

    public MenuClientes() {
        super("GENERADOR DE COMPRADORES");

        conectarAJade();

        JPanel panel = new JPanel();
        panel. setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField campoNombre = crearCampo(panel, "Nombre Cliente:");
        JTextField campoLibros = crearCampo(panel, "Libros (separados por comas):"); // ej: Java,IA,Redes
        JTextField campoDinero = crearCampo(panel, "Presupuesto Máximo por libro:");

        JButton btnCrear = new JButton("CREAR CLIENTE");
        btnCrear.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCrear.setBackground(new Color(60, 179, 113));
        btnCrear.setForeground(Color.WHITE);

        btnCrear.addActionListener(e -> {
            String nombre = campoNombre.getText(). trim();
            String libros = campoLibros.getText().trim();
            String dinero = campoDinero.getText().trim();

            if (! nombre.isEmpty() && !libros.isEmpty() && !dinero. isEmpty()) {
                crearAgente(nombre, libros, dinero);
                // Cerrar la ventana después de crear el cliente
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Rellena todos los datos");
            }
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(btnCrear);

        getContentPane().add(panel);
        setSize(400, 260);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame. DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void conectarAJade() {
        try {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.MAIN_PORT, "1099");
            contenedorAgentes = rt.createAgentContainer(p);
            System.out.println("--- Conectado al sistema JADE correctamente ---");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error conectando a JADE.\nAsegúrate de que el Main Container esté ejecutándose.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void crearAgente(String nombre, String librosComaSep, String dineroStr) {
        try {
            Object[] args = new Object[]{librosComaSep, dineroStr};
            AgentController agente = contenedorAgentes.createNewAgent(nombre, "Comprador", args);
            agente.start();
            System.out.println("Cliente " + nombre + " creado exitosamente.");
            JOptionPane.showMessageDialog(this, "Cliente " + nombre + " creado exitosamente!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear agente: " + nombre + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private JTextField crearCampo(JPanel p, String texto) {
        JLabel l = new JLabel(texto);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
        JTextField tf = new JTextField(20);
        p.add(tf);
        p.add(Box.createVerticalStrut(8));
        return tf;
    }
}