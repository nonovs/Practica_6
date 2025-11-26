import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VendedorGUI extends JFrame {
    private Vendedor myAgent;
    private JTabbedPane tabbedPane;
    private Map<String, JTable> subastaTables = new HashMap<>();
    private Map<String, JTextArea> subastaLogs = new HashMap<>();
    private JTextArea logAreaGeneral;
    private JTextArea logAreaIndividual;

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

        // Panel de pestañas
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Panel derecho para log individual de subasta
        logAreaIndividual = new JTextArea(15, 25);
        logAreaIndividual.setEditable(false);
        JScrollPane logScrollIndividual = new JScrollPane(logAreaIndividual);
        logScrollIndividual.setBorder(BorderFactory.createTitledBorder("Log de Subasta Seleccionada"));
        add(logScrollIndividual, BorderLayout.EAST);

        // Log general abajo
        logAreaGeneral = new JTextArea(8, 40);
        logAreaGeneral.setEditable(false);
        JScrollPane logScrollGeneral = new JScrollPane(logAreaGeneral);
        logScrollGeneral.setBorder(BorderFactory.createTitledBorder("Log General"));
        add(logScrollGeneral, BorderLayout.SOUTH);

        // Cambiar log individual al seleccionar otra pestaña
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx >= 0) {
                String titulo = tabbedPane.getTitleAt(idx);
                JTextArea log = subastaLogs.get(titulo);
                if (log != null) logAreaIndividual.setText(log.getText());
            }
        });

        // Acción del botón iniciar subasta
        btnStart.addActionListener(e -> {
            try {
                String titulo = titleField.getText().trim();
                int precio = Integer.parseInt(priceField.getText().trim());
                int incremento = Integer.parseInt(incField.getText().trim());
                if (titulo.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El título no puede estar vacío.");
                    return;
                }
                myAgent.iniciarSubasta(titulo, precio, incremento);
                titleField.setText("");
                // Crear pestaña para nueva subasta
                addSubastaTab(titulo, precio, 0, "Iniciada");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Introduce números válidos en precio e incremento.");
            }
        });

        setSize(900, 600);
        setLocation(40, 40);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void addSubastaTab(String titulo, int precio, int pujas, String estado) {
        if (subastaTables.containsKey(titulo)) return;

        // Tabla
        String[] cols = {"Precio actual", "Pujas activas", "Estado"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        tableModel.addRow(new Object[]{precio, pujas, estado});
        subastaTables.put(titulo, table);

        // Log individual
        JTextArea logIndividual = new JTextArea();
        logIndividual.setEditable(false);
        logIndividual.append("Subasta " + titulo + " iniciada\n");
        subastaLogs.put(titulo, logIndividual);

        // Panel con tabla y scroll
        JScrollPane scroll = new JScrollPane(table);
        tabbedPane.addTab(titulo, scroll);
    }

    public void addOrUpdateSubasta(String titulo, int precio, int pujas, String estado) {
        SwingUtilities.invokeLater(() -> {
            JTable table = subastaTables.get(titulo);
            if (table != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    model.setValueAt(precio, 0, 0);
                    model.setValueAt(pujas, 0, 1);
                    model.setValueAt(estado, 0, 2);
                } else {
                    model.addRow(new Object[]{precio, pujas, estado});
                }
            }

            // Actualizar logs
            String mensaje = "[" + titulo + "] Precio: " + precio + ", Pujas: " + pujas + ", Estado: " + estado + "\n";
            log(mensaje); // log general

            JTextArea logIndividual = subastaLogs.get(titulo);
            if (logIndividual != null) {
                logIndividual.append(mensaje);
                int idx = tabbedPane.getSelectedIndex();
                if (idx >= 0 && tabbedPane.getTitleAt(idx).equals(titulo)) {
                    logAreaIndividual.setText(logIndividual.getText());
                }
            }
        });
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logAreaGeneral.append(msg);
            logAreaGeneral.append("\n");
            logAreaGeneral.setCaretPosition(logAreaGeneral.getDocument().getLength());
        });
    }
}
