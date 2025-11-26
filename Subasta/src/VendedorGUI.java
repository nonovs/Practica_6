import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VendedorGUI extends JFrame {
    private Vendedor myAgent;
    private DefaultTableModel tableModel;
    private Map<String, Integer> rowIndexByTitle = new HashMap<>();

    public VendedorGUI(Vendedor a) {
        super("Vendedor: " + a.getLocalName());
        myAgent = a;

        setLayout(new BorderLayout(8, 8));

        // Panel para crear nuevas subastas
        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Nueva Subasta"));

        JTextField titleField = new JTextField();
        JTextField priceField = new JTextField("10");
        JTextField incField = new JTextField("5");
        JButton btnStart = new JButton("INICIAR");

        inputPanel.add(new JLabel("Libro:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Precio Inicial:"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Incremento:"));
        inputPanel.add(incField);
        inputPanel.add(btnStart);

        add(inputPanel, BorderLayout.NORTH);

        // Tabla de subastas activas
        String[] cols = {"Libro", "Precio actual", "Pujas activas", "Estado"};
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Subastas activas"));
        add(scroll, BorderLayout.CENTER);

        // Log
        JTextArea logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        add(logScroll, BorderLayout.SOUTH);

        btnStart.addActionListener(e -> {
            try {
                String t = titleField.getText().trim();
                int p = Integer.parseInt(priceField.getText().trim());
                int i = Integer.parseInt(incField.getText().trim());
                if (t.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El título no puede estar vacío.");
                    return;
                }
                myAgent.iniciarSubasta(t, p, i);
                titleField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Introduce números válidos en precio e incremento.");
            }
        });

        setSize(700, 520);
        setLocation(40, 40);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);

        // método para escribir en log (expuesto)
        this.logArea = logArea;
    }

    private JTextArea logArea;

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // Añadir o actualizar subasta en la tabla
    public void addOrUpdateSubasta(String titulo, int precio, int pujas, String estado) {
        SwingUtilities.invokeLater(() -> {
            Integer idx = rowIndexByTitle.get(titulo);
            if (idx == null) {
                rowIndexByTitle.put(titulo, tableModel.getRowCount());
                tableModel.addRow(new Object[]{titulo, precio, pujas, estado});
            } else {
                tableModel.setValueAt(precio, idx, 1);
                tableModel.setValueAt(pujas, idx, 2);
                tableModel.setValueAt(estado, idx, 3);
            }
        });
    }
}
