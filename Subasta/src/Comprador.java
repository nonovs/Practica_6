import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class Comprador extends Agent {
    private String nombreLibro;
    private int precioMax;
    private CompradorGUI compradorGUI;

    protected void setup() {
        // Recuperar argumentos pasados por el Lanzador
        Object[] args = getArguments();

        if (args != null && args.length >= 2) {
            nombreLibro = (String) args[0];
            precioMax = Integer.parseInt((String) args[1]);

            // Nace ya con su ventanita de Log
            compradorGUI = new CompradorGUI(this, nombreLibro, precioMax);
            compradorGUI.log("Agente " + getLocalName() + " creado.");

            // Registro automático
            registrarseEnDF();

            // A escuchar ofertas
            addBehaviour(new GestorPujas());
        } else {
            System.out.println("Error: Comprador lanzado sin argumentos.");
            doDelete();
        }
    }
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        if (compradorGUI != null) {
            compradorGUI.dispose();
        }
        System.out.println("Comprador " + getLocalName() + " terminando.");
    }
    private void registrarseEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("subasta-libros");
        sd.setName("SUBASTA-JADE");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }



    private class GestorPujas extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.CFP) {
                    String contido = msg.getContent();
                    String[] partes = contido.split(":");
                    String tituloLibro = partes[0];
                    int price = Integer.parseInt(partes[1]);

                    compradorGUI.log("Oferta: " + tituloLibro + " a " + price + "€");

                    if (tituloLibro.equalsIgnoreCase(nombreLibro)) {
                        ACLMessage reply = msg.createReply();
                        if (price <= precioMax) {
                            reply.setPerformative(ACLMessage.PROPOSE);
                            reply.setContent(String.valueOf(price));
                            compradorGUI.log(" -> ¡PUJO!");
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            compradorGUI.log(" -> Muy caro. Paso.");
                        }
                        myAgent.send(reply);
                    } else {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REFUSE);
                        myAgent.send(reply);
                    }
                }
                else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    compradorGUI.log("***************************");
                    compradorGUI.log("¡¡GANÉ EL LIBRO!!");
                    compradorGUI.log("***************************");
                }
            } else {
                block();
            }
        }
    }
}
/// COmpñetada
