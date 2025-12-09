import jade.core.Agent;
import jade.wrapper.AgentController;
import javax.swing.*;
import java.awt.*;

public class MenuClientesIntegrado extends JFrame {
    private Agent agenteLauncher;

    public MenuClientesIntegrado(Agent launcher) {
        super("GENERADOR DE COMPRADORES (Integrado)");
        this.agenteLauncher = launcher;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField campoNombre = crearCampo(panel, "Nombre Cliente:");

        // Instrucciones
        JLabel lblInstrucciones = new JLabel("<html><b>Formato:</b> Libro:Precio,Libro: Precio<br>" +
                "Ejemplo: Java:50,IA:80,Redes:30</html>");
        lblInstrucciones.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblInstrucciones.setForeground(new Color(50, 50, 150));
        panel.add(lblInstrucciones);
        panel.add(Box.createVerticalStrut(8));

        JTextArea campoLibros = new JTextArea(3, 20);
        campoLibros.setLineWrap(true);
        campoLibros.setWrapStyleWord(true);
        JScrollPane scrollLibros = new JScrollPane(campoLibros);
        JLabel lblLibros = new JLabel("Libros y Presupuestos:");
        lblLibros.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lblLibros);
        panel.add(scrollLibros);
        panel.add(Box.createVerticalStrut(8));

        JButton btnCrear = new JButton("CREAR CLIENTE");
        btnCrear.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCrear.setBackground(new Color(60, 179, 113));
        btnCrear.setForeground(Color.WHITE);

        btnCrear.addActionListener(e -> {
            String nombre = campoNombre. getText().trim();
            String libros = campoLibros.getText().trim();

            if (!nombre. isEmpty() && !libros.isEmpty()) {
                crearComprador(nombre, libros);
                // Limpar campos después de crear
                campoNombre.setText("");
                campoLibros.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Rellena todos los datos");
            }
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(btnCrear);

        getContentPane().add(panel);
        setSize(450, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void crearComprador(String nombre, String librosYPrecios) {
        try {
            Object[] args = new Object[]{librosYPrecios};
            AgentController comprador = agenteLauncher. getContainerController()
                    .createNewAgent(nombre, "Comprador", args);
            comprador.start();
            System.out.println("Cliente " + nombre + " creado exitosamente desde JADE.");
            JOptionPane.showMessageDialog(this, "Cliente " + nombre + " creado!");
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