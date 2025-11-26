import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Vendedor extends Agent {
    private VendedorGUI myGui;
    // Map: titulo -> comportamiento de subasta
    private Map<String, FuncionamientoSubasta> subastasActivas = new ConcurrentHashMap<>();

    protected void setup() {
        myGui = new VendedorGUI(this);
        myGui.log("Vendedor listo. Usa la GUI para iniciar subastas.");
    }

    // Llamado por la GUI para iniciar una nueva subasta
    public synchronized void iniciarSubasta(String titulo, int precioInicial, int incremento) {
        if (subastasActivas.containsKey(titulo)) {
            myGui.log("Ya existe una subasta para: " + titulo);
            return;
        }
        FuncionamientoSubasta sub = new FuncionamientoSubasta(this, titulo, precioInicial, incremento);
        subastasActivas.put(titulo, sub);
        addBehaviour(sub);
        myGui.log("Subasta creada: " + titulo);
        myGui.addOrUpdateSubasta(titulo, precioInicial, 0, "Iniciada");
    }

    protected void takeDown() {
        myGui.log("Vendedor terminando.");
    }

    // Subclase que gestiona una sola subasta
    private class FuncionamientoSubasta extends Behaviour {
        private final Agent agent;
        private final String libro;
        private int precioActual;
        private final int incremento;
        private int paso = 0;
        private AID[] compradores = new AID[0];
        private MessageTemplate mt;
        private int respuestasRecibidas;
        private int numPropuestas;
        private AID posibleGanador;
        private AID ganadorAnterior;
        private long esperaDecision;
        private boolean terminado = false;

        public FuncionamientoSubasta(Agent a, String titulo, int p, int inc) {
            this.agent = a;
            this.libro = titulo;
            this.precioActual = p;
            this.incremento = inc;
        }

        public void action() {
            switch (paso) {
                case 0:
                    // Buscar compradores interesados (dinámico cada ronda)
                    try {
                        DFAgentDescription dfd = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("subasta-libros");
                        dfd.addServices(sd);
                        DFAgentDescription[] result = DFService.search(agent, dfd);
                        compradores = Arrays.stream(result).map(DFAgentDescription::getName).toArray(AID[]::new);
                    } catch (FIPAException fe) {
                        compradores = new AID[0];
                    }
                    if (compradores.length == 0) {
                        myGui.log("[" + libro + "] No hay compradores conectados. Esperando y reintentando...");
                        myGui.addOrUpdateSubasta(libro, precioActual, 0, "Esperando compradores");
                        block(2000); // reintentar tras 2s
                        return;
                    } else {
                        myGui.log("[" + libro + "] Participantes encontrados: " + compradores.length);
                    }
                    paso = 1;
                    break;

                case 1:
                    // Enviar CFP a todos los compradores
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID c : compradores) cfp.addReceiver(c);
                    cfp.setContent(libro + ":" + precioActual);
                    cfp.setConversationId("subasta-" + libro);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    agent.send(cfp);
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("subasta-" + libro),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                    );
                    respuestasRecibidas = 0;
                    numPropuestas = 0;
                    posibleGanador = null;
                    myGui.addOrUpdateSubasta(libro, precioActual, 0, "Oferta lanzada");
                    paso = 2;
                    break;

                case 2:
                    // Recibir respuestas
                    ACLMessage msg = agent.receive(mt);
                    if (msg != null) {
                        respuestasRecibidas++;
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            numPropuestas++;
                            posibleGanador = msg.getSender();
                            myGui.log("[" + libro + "] Puja recibida de " + msg.getSender().getLocalName());
                            myGui.addOrUpdateSubasta(libro, precioActual, numPropuestas, "Pujas: " + numPropuestas);
                        }
                        if (respuestasRecibidas >= compradores.length) {
                            paso = 3;
                            esperaDecision = System.currentTimeMillis() + 5000; // esperar 5s para decidir
                            myGui.log("[" + libro + "] Esperando " + 5 + "s para decidir...");
                        }
                    } else {
                        block();
                    }
                    break;

                case 3:
                    if (System.currentTimeMillis() < esperaDecision) {
                        block(500);
                        return;
                    }
                    // Evaluar resultados
                    if (numPropuestas == 0) {
                        if (ganadorAnterior != null) {
                            int precioVenta = precioActual - incremento;
                            myGui.log("[" + libro + "] ¡VENDIDO! Ganador: " + ganadorAnterior.getLocalName() + " por " + precioVenta + "€");
                            informarGanador(ganadorAnterior, precioVenta);
                        } else {
                            myGui.log("[" + libro + "] Subasta sin éxito. Nadie pujó.");
                            notificarFinalSinLote();
                        }
                        terminado = true;
                    } else if (numPropuestas == 1) {
                        myGui.log("[" + libro + "] ¡VENDIDO! Ganador: " + posibleGanador.getLocalName() + " por " + precioActual + "€");
                        informarGanador(posibleGanador, precioActual);
                        terminado = true;
                    } else {
                        precioActual += incremento;
                        ganadorAnterior = posibleGanador;
                        myGui.log("[" + libro + "] " + numPropuestas + " pujaron. Subimos a " + precioActual + "€");
                        myGui.addOrUpdateSubasta(libro, precioActual, numPropuestas, "Subiendo precio");
                        paso = 0; // repetir ronda
                    }
                    break;
            }
        }

        private void informarGanador(AID ganador, int precioVenta) {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(ganador);
            accept.setContent(libro + ":" + precioVenta);
            accept.setConversationId("subasta-" + libro);
            agent.send(accept);

            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            for (AID c : compradores) {
                if (!c.equals(ganador)) info.addReceiver(c);
            }
            info.setContent("FINALIZADA:" + libro + ":" + ganador.getLocalName() + ":" + precioVenta);
            info.setConversationId("subasta-" + libro);
            agent.send(info);

            myGui.addOrUpdateSubasta(libro, precioActual, 0, "Finalizada - Vendido a " + ganador.getLocalName());
            subastasActivas.remove(libro);
        }

        private void notificarFinalSinLote() {
            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            for (AID c : compradores) info.addReceiver(c);
            info.setContent("FINALIZADA-SIN-VENTA:" + libro);
            info.setConversationId("subasta-" + libro);
            agent.send(info);
            myGui.addOrUpdateSubasta(libro, precioActual, 0, "Finalizada - Sin venta");
            subastasActivas.remove(libro);
        }

        public boolean done() {
            return terminado;
        }
    }
}
