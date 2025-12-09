import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;

public class CompradorGUI extends JFrame {
    private Comprador comprador;
    private JTextArea logArea;
    private DefaultListModel<String> listaModel;
    private int presupuestoGlobal;
    //mapa de estado por libro
    private Map<String, String> estados = new HashMap<>();

    public CompradorGUI(Comprador a, Map<String, Integer> intereses) {
        super(a.getLocalName());
        comprador = a;
        if (!intereses.isEmpty()) {//Pa coller el presupyesto de todos los libros (es para todos el mismo)
            this.presupuestoGlobal = intereses.values().iterator().next();
        }
        JPanel top = new JPanel(new BorderLayout());
        StringBuilder sb = new StringBuilder("<html><b>Intereses:</b> "); //LO pongo en formato de html, y la b es para ponerlo en negrita
        intereses.keySet().forEach(k -> sb. append(k). append(" "));
        sb.append("</html>");
        top.add(new JLabel(sb.toString()), BorderLayout.NORTH);

        listaModel = new DefaultListModel<>();
        JList<String> lista = new JList<>(listaModel);
        JScrollPane listScroll = new JScrollPane(lista);
        listScroll. setBorder(BorderFactory.createTitledBorder("Estado de subastas"));

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        getContentPane().setLayout(new BorderLayout(6,6));
        getContentPane(). add(top, BorderLayout. NORTH);
        getContentPane(). add(listScroll, BorderLayout.CENTER);
        getContentPane().add(logScroll, BorderLayout.SOUTH);

        setSize(320, 420);
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
            estados.put(titulo, estado);
            rebuildList();
        });
    }

    private void rebuildList() {
        listaModel.clear();

        // Bucle FOR "tradicional" (Opción keySet)
        for (String titulo : estados.keySet()) {
            // 1. Definimos la variable 'estado' recuperándola del mapa
            String estado = estados.get(titulo);

            // 2. Ahora ya existen 'titulo' y 'estado', así que esta línea funcionará
            String linea = String.format("%s [Max: %d€] : %s", titulo.toUpperCase(), presupuestoGlobal, estado);

            // 3. Lo añadimos a la lista
            listaModel.addElement(linea);
        }

    }
}