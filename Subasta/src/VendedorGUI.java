import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class VendedorGUI extends JFrame {
    private Vendedor myAgent;
    private JTabbedPane tabbedPane;

    // Maps para gestionar las tablas y logs por título del libro
    private Map<String, JTable> subastaTables = new HashMap<>();
    private Map<String, JTextArea> subastaLogs = new HashMap<>();

    private JTextArea logAreaGeneral;
    private JTextArea logAreaIndividual;
    private BufferedImage fondoImagen;

    public VendedorGUI(Vendedor a) {
        super("Vendedor: " + a.getLocalName());
        myAgent = a;

        // Cargar la imagen de fondo
        try {
            fondoImagen = ImageIO.read(new File("fondo.jpg"));
        } catch (Exception e) {
            System.out.println("No se pudo cargar la imagen de fondo: " + e.getMessage());
        }

        // Panel principal con imagen de fondo
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (fondoImagen != null) {
                    // Dibujar la imagen escalada al tamaño del panel
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints. VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(fondoImagen, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        mainPanel.setOpaque(false);

        setContentPane(mainPanel);

        // --- Panel Superior: Crear Subasta ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Gestión de Subastas"));

        // 1. Sub-panel para los campos (FlowLayout alinea todo en una fila centrada)
        // El '20' es el espacio horizontal entre elementos
        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));

        JTextField titleField = new JTextField();
        titleField.setColumns(15); // IMPORTANTE: Definir ancho visual

        JTextField priceField = new JTextField("10");
        priceField.setColumns(5);

        JTextField incField = new JTextField("5");
        incField.setColumns(5);

        // Añadimos pares Etiqueta-Campo al sub-panel
        fieldsPanel.add(new JLabel("Libro:"));
        fieldsPanel.add(titleField);
        fieldsPanel.add(new JLabel("Precio Inicial:"));
        fieldsPanel.add(priceField);
        fieldsPanel.add(new JLabel("Incremento:"));//a
        fieldsPanel.add(incField);

        // 2. Sub-panel para el botón
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnCrear = new JButton("CREAR E INICIAR SUBASTA");
        btnCrear.setBackground(new Color(60, 179, 113));
        btnCrear.setForeground(Color.WHITE);
        buttonPanel.add(btnCrear);

        // Añadimos los dos sub-paneles al panel superior
        inputPanel.add(fieldsPanel);
        inputPanel.add(buttonPanel);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // --- Panel Central: Pestañas ---
        tabbedPane = new JTabbedPane();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // --- Panel Derecho: Log Individual ---
        logAreaIndividual = new JTextArea(15, 25);
        logAreaIndividual.setEditable(false);
        JScrollPane logScrollIndividual = new JScrollPane(logAreaIndividual);
        logScrollIndividual.setBorder(BorderFactory.createTitledBorder("Log de Subasta Seleccionada"));
        mainPanel.add(logScrollIndividual, BorderLayout.EAST);

        // --- Panel Inferior: Log General ---
        logAreaGeneral = new JTextArea(8, 40);
        logAreaGeneral.setEditable(false);
        JScrollPane logScrollGeneral = new JScrollPane(logAreaGeneral);
        logScrollGeneral.setBorder(BorderFactory.createTitledBorder("Log General"));
        mainPanel.add(logScrollGeneral, BorderLayout.SOUTH);

        // EVENTO: Cambiar log individual al cambiar de pestaña
        tabbedPane.addChangeListener(e -> {
            actualizarLogIndividualVisible();
        });

        // ACCIÓN: Botón CREAR E INICIAR
        btnCrear.addActionListener(e -> {
            try {
                String titulo = titleField.getText(). trim();
                int precio = Integer.parseInt(priceField.getText().trim());
                int incremento = Integer.parseInt(incField. getText().trim());

                if (titulo.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El título no puede estar vacío.");
                    return;
                }
                if (subastaTables.containsKey(titulo)) {
                    JOptionPane.showMessageDialog(this, "Ya existe una subasta con ese título.");
                    return;
                }

                // 1. Crear la pestaña en la GUI
                addSubastaTab(titulo, precio, 0, "Iniciando...");

                // 2. Almacenar e iniciar inmediatamente en el Agente
                if (myAgent != null) {
                    myAgent.almacenarSubasta(titulo, precio, incremento);
                    boolean iniciada = myAgent.iniciarSubastaEspecifica(titulo);
                    if (iniciada) {
                        log("Subasta creada e iniciada: " + titulo);
                    } else {
                        log("Error: No se pudo iniciar la subasta " + titulo);
                    }
                }

                // Limpiar campos
                titleField.setText("");
                priceField.setText("10");
                incField.setText("5");

            } catch (NumberFormatException ex) {
                JOptionPane. showMessageDialog(this, "Introduce números válidos en precio e incremento.");
            }
        });

        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants. DISPOSE_ON_CLOSE);
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

        // Log individual (oculto, se guarda en memoria)
        JTextArea logIndividual = new JTextArea();
        logIndividual.setEditable(false);
        logIndividual.append("--- Subasta: " + titulo + " ---\n");
        subastaLogs.put(titulo, logIndividual);

        // Panel con tabla y scroll
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        table.setOpaque(true);
        table.setBackground(new Color(255, 255, 255, 230)); // Fondo blanco semi-transparente

        tabbedPane.addTab(titulo, scroll);

        // Seleccionar la nueva pestaña creada para verla inmediatamente
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    public void addOrUpdateSubasta(String titulo, int precio, int pujas, String estado) {
        SwingUtilities.invokeLater(() -> {
            // 1. Actualizar tabla
            JTable table = subastaTables.get(titulo);
            if (table != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    model.setValueAt(precio, 0, 0);
                    model.setValueAt(pujas, 0, 1);
                    model. setValueAt(estado, 0, 2);
                }
            }

            // 2.  Actualizar Logs
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

    // Metodo auxiliar para refrescar el panel derecho segun la pestaña actual
    private void actualizarLogIndividualVisible() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx >= 0) {
            String titulo = tabbedPane.getTitleAt(idx);
            JTextArea logGuardado = subastaLogs.get(titulo);
            if (logGuardado != null) {
                logAreaIndividual.setText(logGuardado.getText());
                logAreaIndividual.setCaretPosition(logAreaIndividual.getDocument(). getLength());
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