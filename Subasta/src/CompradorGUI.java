import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class CompradorGUI extends JFrame {
    private Comprador comprador;
    private JTextArea logArea;

    CompradorGUI(Comprador a, String book, int money) {
        super(a.getLocalName()); // El título de la ventana es el nombre del agente
        comprador = a;

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(new Color(255, 250, 205)); // Amarillo suave
        infoPanel.add(new JLabel("  Buscando: " + book));
        infoPanel.add(new JLabel("  Presupuesto: " + money));

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);

        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { comprador.doDelete(); }
        });

        setSize(250, 350);
        // Posición aleatoria
        setLocation((int)(Math.random()*800), (int)(Math.random()*500)+100);
        setVisible(true);
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}