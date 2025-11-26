import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.AID;

public class Vendedor extends Agent {
    private VendedorGUI myGui;

    protected void setup() {
        myGui = new VendedorGUI(this);
        myGui.log("Vendedor listo. Esperando iniciar subasta...");
    }



    public void iniciarSubasta(String titulo, int precio, int inc) {
        addBehaviour(new FuncionamientoSubasta(this, titulo, precio, inc));
    }

    private class FuncionamientoSubasta extends Behaviour {
        private String Libro;
        private int precioactual, incremento, pasos = 0;
        private AID[] compradores;
        private int respuestas_clientes = 0;
        private MessageTemplate mt;
        private AID posibleganador = null;
        private long espera;
        private int num_propuestas = 0;
        private AID ganador_anterior = null;

        public FuncionamientoSubasta(Agent a, String t, int p, int i) {
            super(a);
            Libro = t; precioactual = p; incremento = i;
            myGui.log("--- NUEVA SUBASTA: " + t + " ---");
        }

        public void action() {
            switch (pasos) {
                case 0: // Buscar compradores
                    DFAgentDescription descripcionagente = new DFAgentDescription();
                    ServiceDescription servicio = new ServiceDescription();
                    servicio.setType("subasta-libros");
                    descripcionagente.addServices(servicio);
                    try {
                        DFAgentDescription[] resultado = DFService.search(myAgent, descripcionagente);
                        compradores = new AID[resultado.length];
                        for (int i = 0; i < resultado.length; ++i) compradores[i] = resultado[i].getName();

                        if (compradores.length > 0) {
                            myGui.log("Participantes encontrados: " + compradores.length);
                            pasos = 1;
                        } else {
                            myGui.log("No hay nadie conectado.");
                            pasos = 4;
                        }
                    } catch (FIPAException fe) { fe.printStackTrace(); }
                    break;

                case 1: // Enviar oferta
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID comprador : compradores) cfp.addReceiver(comprador);
                    cfp.setContent(Libro + ":" + precioactual);
                    cfp.setConversationId("subasta-" + Libro);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.MatchConversationId("subasta-" + Libro);

                    myGui.log("Oferta lanzada: " + precioactual + "€");
                    pasos = 2; respuestas_clientes = 0; num_propuestas = 0;
                    break;

                case 2: // Recibir respuestas
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            num_propuestas++;
                            posibleganador = msg.getSender();
                            myGui.log(" -> Puja recibida de " + msg.getSender().getLocalName());
                        }
                        respuestas_clientes++;
                        if (respuestas_clientes >= compradores.length) {
                            pasos = 3;
                            espera = System.currentTimeMillis() + 10000;
                            myGui.log("Esperando 10s para decidir...");
                        }
                    } else block();
                    break;

                case 3: // Decidir
                    if (System.currentTimeMillis() < espera) { block(1000); return; }

                    if (num_propuestas == 0) {
                        if (ganador_anterior != null) {
                            myGui.log("¡VENDIDO! Ganador: " + ganador_anterior.getLocalName() + " por " + (precioactual - incremento));
                            informarGanador(ganador_anterior);
                        } else {
                            myGui.log("Subasta sin éxito. Nadie pujó.");
                        }
                        pasos = 4;
                    } else if (num_propuestas == 1) {
                        myGui.log("¡VENDIDO! Solo queda " + posibleganador.getLocalName() + ". Precio: " + precioactual);
                        informarGanador(posibleganador);
                        pasos = 4;
                    } else {
                        precioactual += incremento;
                        ganador_anterior = posibleganador;
                        myGui.log("Mucha gente. Subimos a " + precioactual);
                        pasos = 1;
                    }
                    break;
            }
        }

        private void informarGanador(AID winner) {
            ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            msg.addReceiver(winner);
            msg.setContent(Libro);
            msg.setConversationId("subasta-" + Libro);
            myAgent.send(msg);
        }
        public boolean done() { return pasos == 4; }
    }
}
//COmpleta
