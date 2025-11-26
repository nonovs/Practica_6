import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Comprador extends Agent {
    private Map<String, Integer> presupuestoPorLibro = new ConcurrentHashMap<>();
    private CompradorGUI compradorGUI;

    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length < 2) {
            System.out.println("Comprador lanzado sin argumentos correctos.");
            doDelete();
            return;
        }
        // args[0] = "Java,IA", args[1] = "50"
        String librosRaw = (String) args[0];
        String dineroStr = (String) args[1];

        String[] libros = Arrays.stream(librosRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        int presupuesto;
        try {
            presupuesto = Integer.parseInt(dineroStr);
        } catch (NumberFormatException e) {
            presupuesto = 100; // fallback
        }

        // Guardar presupuesto por cada libro
        for (String l : libros){
            presupuestoPorLibro.put(l.toLowerCase(), presupuesto);//Poño directamente todos en minuscula pa que non haia erros
        }

        compradorGUI = new CompradorGUI(this, presupuestoPorLibro);
        compradorGUI.log("Agente " + getLocalName() + " creado. Intereses: " + Arrays.toString(libros));

        registrarseEnDF();

        // Behaviour principal: recibir mensajes relacionados con subastas
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void registrarseEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("subasta-libros"); // genérico: vendedor hace CFP a todos los "subasta-libros"
        sd.setName("SUBASTA-GLOBAL");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void handleMessage(ACLMessage msg) {
        int perf = msg.getPerformative();
        String convId = msg.getConversationId();
        String content = msg.getContent();

        // CFP: "Libro:precio"
        if (perf == ACLMessage.CFP && content != null && content.contains(":")) {
            String[] parts = content.split(":");
            String titulo = parts[0].toLowerCase();//Nombres a minuscula todos
            int precio = Integer.parseInt(parts[1]);

            compradorGUI.log("Oferta: " + titulo + " a " + precio + "€");

            if (presupuestoPorLibro.containsKey(titulo)) {
                int presupuesto = presupuestoPorLibro.get(titulo);
                ACLMessage reply = msg.createReply();
                if (precio <= presupuesto) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(precio));
                    compradorGUI.log(" -> ¡PUJO en " + titulo + " a " + precio + "€!");
                    compradorGUI.updateSubastaStatus(titulo, "Pujando: " + precio + "€");
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("TOO_EXPENSIVE");
                    compradorGUI.updateSubastaStatus(titulo, "Muy caro: " + precio + "€");
                }
                send(reply);
            } else {
                // No interesado -> ignorar o responder REFUSE
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("NO_INTERESADO");
                send(reply);
            }
        } else if (perf == ACLMessage.ACCEPT_PROPOSAL) {
            // Ganador
            compradorGUI.log("***************************");
            compradorGUI.log("¡¡GANÉ " + content + "!!");
            compradorGUI.log("***************************");
            compradorGUI.updateSubastaStatus(content, "¡GANADO!");
            // Podríamos doDelete() o seguir para otras subastas; lo dejamos activo
        } else if (perf == ACLMessage.INFORM && content != null) {
            // Contenido: "FINALIZADA:Libro:Ganador:precio" o "FINALIZADA-SIN-VENTA:Libro"
            if (content.startsWith("FINALIZADA:")) {
                String[] c = content.split(":");
                String libro = c[1];
                String ganador = c[2];
                String precio = c[3];
                if (ganador.equals(getLocalName())) {
                    compradorGUI.log("He ganado " + libro + " por " + precio + "€");
                    compradorGUI.updateSubastaStatus(libro, "GANADO por " + precio + "€");
                } else {
                    compradorGUI.log("Subasta finalizada. " + libro + " vendido a " + ganador + " por " + precio + "€");
                    compradorGUI.updateSubastaStatus(libro, "Perdido - vendido a " + ganador);
                }
            } else if (content.startsWith("FINALIZADA-SIN-VENTA:")) {
                String[] c = content.split(":");
                String libro = c[1];
                compradorGUI.log("Subasta finalizada sin venta: " + libro);
                compradorGUI.updateSubastaStatus(libro, "Finalizada - sin venta");
            } else {
                compradorGUI.log("INFO: " + content);
            }
        } else {
            // otros mensajes
            compradorGUI.log("Mensaje recibido: " + msg.getContent());
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // ignore
        }
        if (compradorGUI != null) compradorGUI.dispose();
        System.out.println("Comprador " + getLocalName() + " terminando.");
    }
}
