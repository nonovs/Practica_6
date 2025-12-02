import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;

public class CompradorGUI extends JFrame {
    private Comprador comprador;
    private JTextArea logArea;
    private DefaultListModel<String> listaModel;
    //mapa de estado por libro
    private Map<String, String> estados = new HashMap<>();

    public CompradorGUI(Comprador a, Map<String, Integer> intereses) {
        super(a.getLocalName());
        comprador = a;

        JPanel top = new JPanel(new BorderLayout());
        StringBuilder sb = new StringBuilder("<html><b>Intereses:</b> ");
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
        listaModel. clear();
        estados.forEach((k,v) -> listaModel.addElement(k + " : " + v));
    }
}