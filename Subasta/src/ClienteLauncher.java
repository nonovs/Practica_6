import jade.core.Agent;
import javax.swing.*;

public class ClienteLauncher extends Agent {

    protected void setup() {
        // Abre la interfaz de creación de clientes al iniciar el agente
        SwingUtilities.invokeLater(() -> {
            new MenuClientesIntegrado(this);
        });
    }
}