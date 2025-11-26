import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class VendedorGUI extends JFrame {
    private Vendedor myAgent;
    private JTextField titleField, priceField, incField;
    private JTextArea logArea; // Aquí saldrá el texto

    VendedorGUI(Vendedor a) {
        super("Vendedor: " + a.getLocalName());
        myAgent = a;

        // 1. Panel Superior (Datos)
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5)); // Rejilla simple
        inputPanel.setBorder(BorderFactory.createTitledBorder("Nueva Subasta"));

        inputPanel.add(new JLabel(" Libro:"));
        titleField = new JTextField("Java"); // Valor por defecto
        inputPanel.add(titleField);

        inputPanel.add(new JLabel(" Precio Inicial:"));
        priceField = new JTextField("10");
        inputPanel.add(priceField);

        inputPanel.add(new JLabel(" Incremento:"));
        incField = new JTextField("10");
        inputPanel.add(incField);

        JButton btnStart = new JButton("INICIAR");
        inputPanel.add(btnStart);

        JButton btnClear = new JButton("LIMPIAR LOG");
        inputPanel.add(btnClear);

        // 2. Panel Central (Log de texto)
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Estado de la Subasta"));

        // 3. Montar la ventana
        getContentPane().add(inputPanel, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);

        // Acciones de los botones
        btnStart.addActionListener(ev -> {
            try {
                String t = titleField.getText();
                int p = Integer.parseInt(priceField.getText());
                int i = Integer.parseInt(incField.getText());
                myAgent.iniciarSubasta(t, p, i);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Introduce números válidos.");
            }
        });

        btnClear.addActionListener(ev -> logArea.setText(""));

        // Configuración final
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { myAgent.doDelete(); }
        });

        setSize(400, 500);
        setLocation(50, 50); // Posición en pantalla
        setVisible(true);
    }

    // Método para escribir en la pantalla desde el agente
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}