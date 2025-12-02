import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.*;


public class Comprador extends Agent {
    //Mapa donde guardo cuanto dinero tengo para cada libro
    private Map<String, Integer> presupuestoPorLibro = new HashMap<>();
    private CompradorGUI compradorGUI;

    protected void setup() {
        //cojo los argumentos que me pasan al crear el agente
        Object[] args = getArguments();
        if (args == null || args.length < 2) {
            System.out.println("Comprador lanzado sin argumentos correctos.");
            doDelete();
            return;
        }
        // args[0] = "Java,IA", args[1] = "50"
        String librosRaw = (String) args[0];
        String dineroStr = (String) args[1];

        //separo los libros por comas y quito espacios
        String[] partes = librosRaw.split(",");
        List<String> listaTemp = new ArrayList<>();
        for (String s : partes) {
            if (!s.trim().isEmpty()) {
                listaTemp.add(s.trim());
            }
        }
        String[] libros = listaTemp.toArray(new String[0]);

        int presupuesto;
        try {
            presupuesto = Integer.parseInt(dineroStr);
        } catch (NumberFormatException e) {
            presupuesto = 100; //Si no ponen nada meto 100 por defecto
        }

        //Guardo presupuesto por cada libro en el mapa
        for (String l : libros){
            presupuestoPorLibro.put(l.toLowerCase(), presupuesto);//Pongo directamente todos en minuscula pa que non haya  errores
        }

        //arranco la interfaz grafica del comprador
        compradorGUI = new CompradorGUI(this, presupuestoPorLibro);
        compradorGUI.log("Agente " + getLocalName() + " creado. Intereses: " + Arrays.toString(libros));

        //me registro en DF para que el vendedor me pueda ver
        registrarseEnDF();

        //Behaviour(trabajo que realiza los compradores) principal bucle infinito para recibir mensajes
        addBehaviour(new CyclicBehaviour() {

            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    //si me llega mensaje lo proceso
                    procesarMensaje(msg);
                } else {
                    //si no hay mensaje bloqueo para no gastar cpu
                    block();
                }
            }
        });
    }

    private void registrarseEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("subasta-libros"); //digo que soy tipo subasta-libros para que el vendedor me encuentre
        sd.setName("SUBASTA-GLOBAL");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void procesarMensaje(ACLMessage msg) {
        int perf = msg.getPerformative();
        String convId = msg.getConversationId();
        String content = msg.getContent();

        // CFP: Me llega oferta "Libro:precio"
        if (perf == ACLMessage.CFP && content != null && content.contains(":")) {
            String[] parts = content.split(":");
            String titulo = parts[0].toLowerCase();//Nombres a minuscula todos
            int precio = Integer.parseInt(parts[1]);

            compradorGUI.log("Oferta: " + titulo + " a " + precio + "€");

            //miro si el libro me interesa
            if (presupuestoPorLibro.containsKey(titulo)) {
                int presupuesto = presupuestoPorLibro.get(titulo);
                ACLMessage respuesta = msg.createReply();
                /*Mira presupuestoPorLibro, si precio<= presupuesto-->manda PROPOSE (PUJA)
	               En caso de que NO --> REFUSE NO_INTERESADO
	               Recibe ACCEPT_PROPOSAL si gana ou INFORM para saber quen ganou se perde.
	               */

                //compruebo si tengo dinero suficiente
                if (precio <= presupuesto) {
                    respuesta.setPerformative(ACLMessage.PROPOSE);
                    respuesta.setContent(String.valueOf(precio));
                    compradorGUI.log(" -> ¡PUJO en " + titulo + " a " + precio + "€!");
                    compradorGUI.updateSubastaStatus(titulo, "Pujando: " + precio + "€");
                } else {
                    //si es muy caro paso
                    respuesta.setPerformative(ACLMessage.REFUSE);
                    respuesta.setContent("MUY_CARO");
                    compradorGUI.updateSubastaStatus(titulo, "Muy caro: " + precio + "€");
                }
                send(respuesta);
            } else {
                // No interesado -> ignorar o responder REFUSE
                ACLMessage respuesta = msg.createReply();
                respuesta.setPerformative(ACLMessage.REFUSE);
                respuesta.setContent("NO_INTERESADO");
                send(respuesta);
            }
        } else if (perf == ACLMessage.ACCEPT_PROPOSAL) {
            // Ganador
            compradorGUI.log("***************************");
            compradorGUI.log("¡¡GANÉ " + content + "!!");
            compradorGUI.log("***************************");
            compradorGUI.updateSubastaStatus(content, "¡GANADO!");
            
        } else if (perf == ACLMessage.INFORM && content != null) {
            // Contenido: me informan de como acabo la cosa
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