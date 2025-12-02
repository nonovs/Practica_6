import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VendedorGUI extends JFrame {
    private Vendedor myAgent;
    private JTabbedPane tabbedPane;

    // Maps para gestionar las tablas y logs por título del libro
    private Map<String, JTable> subastaTables = new HashMap<>();
    private Map<String, JTextArea> subastaLogs = new HashMap<>();

    private JTextArea logAreaGeneral;
    private JTextArea logAreaIndividual;

    public VendedorGUI(Vendedor a) {
        super("Vendedor: " + a.getLocalName());
        myAgent = a;

        setLayout(new BorderLayout(8, 8));

        // --- Panel Superior: Crear Subasta ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Gestión de Subastas"));

        JTextField titleField = new JTextField();
        JTextField priceField = new JTextField("10");
        JTextField incField = new JTextField("5");
        JButton btnCrear = new JButton("1. CREAR");
        JButton btnStart = new JButton("2. INICIAR (Seleccionada)");

        inputPanel.add(new JLabel("Libro:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Precio Inicial:"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Incremento:"));
        inputPanel.add(incField);
        inputPanel.add(btnCrear);
        inputPanel.add(btnStart);

        add(inputPanel, BorderLayout.NORTH);

        // --- Panel Central: Pestañas ---
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // --- Panel Derecho: Log Individual ---
        logAreaIndividual = new JTextArea(15, 25);
        logAreaIndividual.setEditable(false);
        JScrollPane logScrollIndividual = new JScrollPane(logAreaIndividual);
        logScrollIndividual.setBorder(BorderFactory.createTitledBorder("Log de Subasta Seleccionada"));
        add(logScrollIndividual, BorderLayout.EAST);

        // --- Panel Inferior: Log General ---
        logAreaGeneral = new JTextArea(8, 40);
        logAreaGeneral.setEditable(false);
        JScrollPane logScrollGeneral = new JScrollPane(logAreaGeneral);
        logScrollGeneral.setBorder(BorderFactory.createTitledBorder("Log General"));
        add(logScrollGeneral, BorderLayout.SOUTH);

        // EVENTO: Cambiar log individual al cambiar de pestaña
        tabbedPane.addChangeListener(e -> {
            actualizarLogIndividualVisible();
        });

        // ACCIÓN: Botón CREAR
        btnCrear.addActionListener(e -> {
            try {
                String titulo = titleField.getText().trim();
                int precio = Integer.parseInt(priceField.getText().trim());
                int incremento = Integer.parseInt(incField.getText().trim());

                if (titulo.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El título no puede estar vacío.");
                    return;
                }
                if (subastaTables.containsKey(titulo)) {
                    JOptionPane.showMessageDialog(this, "Ya existe una subasta con ese título.");
                    return;
                }

                // 1. Almacenar en el Agente (estado Pendiente)
                if (myAgent != null) {
                    myAgent.almacenarSubasta(titulo, precio, incremento);
                }

                // 2. Crear la pestaña en la GUI
                //uso titulo como clave unica
                addSubastaTab(titulo, precio, 0, "Creada (Pendiente)");

                log("Subasta creada: " + titulo);
                titleField.setText(""); //limpio campo

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Introduce números válidos en precio e incremento.");
            }
        });

        // ACCIÓN: Botón INICIAR (Solo la seleccionada)
        btnStart.addActionListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(this, "No hay ninguna subasta seleccionada.");
                return;
            }

            //Obtenemos el título de la pestaña actual
            String tituloSeleccionado = tabbedPane.getTitleAt(idx);

            log("Solicitando iniciar subasta: " + tituloSeleccionado);

            if (myAgent != null) {
                //Llamamos al método del agente para iniciar UNA sola
                boolean iniciada = myAgent.iniciarSubastaEspecifica(tituloSeleccionado);
                if (!iniciada) {
                    log("Aviso: La subasta '" + tituloSeleccionado + "' ya está activa o no se encontró en pendientes.");
                }
            }
        });

        setSize(950, 650);
        setLocationRelativeTo(null); //centrar en pantalla
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

        //Log individual (oculto, se guarda en memoria)
        JTextArea logIndividual = new JTextArea();
        logIndividual.setEditable(false);
        logIndividual.append("--- Subasta: " + titulo + " ---\n");
        subastaLogs.put(titulo, logIndividual);

        // Panel con tabla y scroll
        JScrollPane scroll = new JScrollPane(table);
        tabbedPane.addTab(titulo, scroll); //El título de la pestaña es la CLAVE

        // Seleccionar la nueva pestaña creada para verla inmediatamente
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    public void addOrUpdateSubasta(String titulo, int precio, int pujas, String estado) {
        SwingUtilities.invokeLater(() -> {//StackOverflow me dijo que era mejor ponerlo por tema de gestion de hlos y problemas
            // 1. Actualizar tabla, Los agentes corren en hilos del JADE y la interfaz en el hilo de AWT/SWING, con invokelater pongo la acutalizon en la cola de la interfaz pro seguridad para que no se quede colgado
            JTable table = subastaTables.get(titulo);
            if (table != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    model.setValueAt(precio, 0, 0);
                    model.setValueAt(pujas, 0, 1);
                    model.setValueAt(estado, 0, 2);
                }
            }

            // 2. Actualizar Logs
            String mensaje = String.format("[%s] Precio: %d | Pujas: %d | Estado: %s\n", titulo, precio, pujas, estado);

            // Log General
            logAreaGeneral.append(mensaje);
            logAreaGeneral.setCaretPosition(logAreaGeneral.getDocument().getLength());

            // Log Individual (Memoria)
            JTextArea logInd = subastaLogs.get(titulo);
            if (logInd != null) {
                logInd.append(mensaje);
            } else {
                System.err.println("Error GUI: No encuentro log para la clave '" + titulo + "'");
            }

            // Log Individual (Visual - si es la pestaña activa)
            actualizarLogIndividualVisible();
        });
    }

    //Metodo auxiliar para refrescar el panel derecho segun la pestaña actual
    private void actualizarLogIndividualVisible() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx >= 0) {
            String titulo = tabbedPane.getTitleAt(idx);
            JTextArea logGuardado = subastaLogs.get(titulo);
            if (logGuardado != null) {
                logAreaIndividual.setText(logGuardado.getText());
                logAreaIndividual.setCaretPosition(logAreaIndividual.getDocument().getLength());
            }
        } else {
            logAreaIndividual.setText("");
        }
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logAreaGeneral.append("SISTEMA: " + msg + "\n");
            logAreaGeneral.setCaretPosition(logAreaGeneral.getDocument().getLength());
        });
    }
}