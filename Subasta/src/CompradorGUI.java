import javax.swing.*;
import java.awt.*;
import java. util.Map;
import java. util.HashMap;

public class CompradorGUI extends JFrame {
    private Comprador comprador;
    private JTextArea logArea;
    private DefaultListModel<String> listaModel;
    private Map<String, Integer> presupuestoPorLibro; // Gardar prezos individuais
    //mapa de estado por libro
    private Map<String, String> estados = new HashMap<>();

    public CompradorGUI(Comprador a, Map<String, Integer> intereses) {
        super(a.getLocalName());
        comprador = a;
        this.presupuestoPorLibro = new HashMap<>(intereses); // Gardamos todos os prezos

        JPanel top = new JPanel(new BorderLayout());
        StringBuilder sb = new StringBuilder("<html><b>Intereses: </b> ");

        // Mostrar cada libro co seu prezo individual
        for (Map. Entry<String, Integer> entry :  intereses.entrySet()) {
            sb.append(entry. getKey()).append(":").append(entry.getValue()).append("€  ");
        }
        sb. append("</html>");
        top.add(new JLabel(sb.toString()), BorderLayout.NORTH);

        listaModel = new DefaultListModel<>();
        JList<String> lista = new JList<>(listaModel);
        JScrollPane listScroll = new JScrollPane(lista);//a
        listScroll.setBorder(BorderFactory.createTitledBorder("Estado de subastas"));

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        getContentPane().setLayout(new BorderLayout(6,6));
        getContentPane().add(top, BorderLayout. NORTH);
        getContentPane().add(listScroll, BorderLayout.CENTER);
        getContentPane().add(logScroll, BorderLayout. SOUTH);

        setSize(400, 500);
        //lo pongo en una posicion aleatoria pa que no se solapen
        setLocation((int)(Math.random()*600)+200, (int)(Math.random()*300)+100);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                comprador. doDelete();
            }
        });

        //Inicializar estados
        intereses.keySet().forEach(k -> updateSubastaStatus(k, "Esperando ofertas"));

        setVisible(true);
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea. append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateSubastaStatus(String titulo, String estado) {
        SwingUtilities.invokeLater(() -> {
            String tituloLower = titulo.toLowerCase();

            // Solo actualizar para estados finales importantes
            if (estado.contains("GANADO") ||
                    estado.contains("vendido a") ||
                    estado.contains("Perdido")) {
                estados.put(tituloLower, estado);
                rebuildList();
            }
        });
    }

    private void rebuildList() {
        listaModel.clear();

        // Reconstruir lista con libro, presupuesto y estado
        for (String titulo : estados.keySet()) {
            String estado = estados.get(titulo);
            Integer presupuesto = presupuestoPorLibro. get(titulo);

            if (presupuesto != null) {
                // Formato:  "java [50€] - Pujando:  10€"
                String linea = titulo + " [" + presupuesto + "€] - " + estado;
                listaModel.addElement(linea);
            } else {
                // Si no hay presupuesto guardado (raro), mostrar solo estado
                listaModel. addElement(titulo + " - " + estado);
            }
        }
    }
}