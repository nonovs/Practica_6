import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class Vendedor extends Agent {
    private VendedorGUI myGui;


    // subastasActivas: Map con los procesos (Behaviours) que están en ejecución
    private Map<String, FuncionamientoSubasta> subastasActivas = new HashMap<>();//Asi mantengo referencia a los procesos,
    //Me permite saber que libros se estan subastando ahora y evita que dos subastas iguales vallan al mismo libro

    //Jade estandar el agente es monohilo por ello no me hace falta lo de concurrentHashMap
    // pendientes: Datos de subastas creadas en la GUI pero que aún no iniciados
    private final Map<String, SubastaInfo> pendientes = new HashMap<>();

    //Clase auxiliar para alamacenar los datos de la subasta de un determinado libro
    private static class SubastaInfo {
        String titulo;
        int precioInicial;
        int incremento;

        SubastaInfo(String t, int p, int i) {//Asi gestiono de forma mas comoda todos los datos de los libros
            this.titulo = t;
            this.precioInicial = p;
            this.incremento = i;
        }
    }

    // Creo el agente
    protected void setup() {
        //Inicializo la ventana del agente
        javax.swing.SwingUtilities.invokeLater(() -> {myGui = new VendedorGUI(this);myGui.log("Agente Vendedor iniciado: " + getLocalName());});
    }


    //Funcion de los botones

    // Botón "Crear": guarda los datos (e inicia la subasta)
    public void almacenarSubasta(String titulo, int precio, int inc) {
        pendientes.put(titulo, new SubastaInfo(titulo, precio, inc));
    }

    //Saca los datos de pendientes y le mete en (Behaviour) al agente.
    public boolean iniciarSubastaEspecifica(String titulo) {
        SubastaInfo info = pendientes.remove(titulo);

        if (info != null) {
            //Creo  lógica de la subasta para el libro y la añado a la cola de tareas del agente
            FuncionamientoSubasta behaviour = new FuncionamientoSubasta(this, info.titulo, info.precioInicial, info.incremento);
            subastasActivas.put(titulo, behaviour);
            addBehaviour(behaviour);
            if(myGui != null) myGui.log("Comportamiento añadido para: " + titulo);
            return true;
        }
        return false;
    }


    //Clase donde gestiono como se vende un libro
    private class FuncionamientoSubasta extends Behaviour {
        private Agent agent;
        private String libro;
        private int precioActual;
        private int incremento;
        private int paso = 0; // Controla en qué fase esta el agente (0:Inicio, 1:Preguntar, 2:Escuchar, 3:Decidir)

        private AID[] compradores = new AID[0]; //Lista de compradores interesados
        private MessageTemplate mt;// Filtro para coger solo los mensajes que interesan
        private int respuestasRecibidas;//Contador de respuestas
        private int numPropuestas;// Contador de personas que han entrado a la puja

        private AID posibleGanador; //Primera persona que ha pujado en esta ronda
        private AID ganadorAnterior;//El que ganó la ronda anterior (en caso de que en la siguiente ronda no pujase nadie)

        private long esperaDecision;     // Variable para que haya una espera de 10 segundos
        private boolean terminado = false; // Para saber cuándo se termino la puja de un libro
        public FuncionamientoSubasta(Agent a, String titulo, int p, int inc) {
            super(a);
            this.agent = a;
            this.libro = titulo;
            this.precioActual = p;//a
            this.incremento = inc;
        }

        // Este método  se ejecuta en bucle infinito hasta que se llame a "done()"
        public void action() {
            switch (paso) {
                case 0:
                    // Simplemente imprimo  en el log de que comienza la subasta
                    myGui.log("--- Iniciando subasta del libro: " + libro + " ---");
                    paso = 1; //VOy al paso 1
                    break;

                case 1: 

                    // 1. Consulto DF
                    // Hago esto en cada ronda para que si un jugador entra en la ronda 5 lo tenga también en cuenta
                    //DFService.search para que me busque todos os compradores, asi podo meter compradores novos incluso a mitad de subasta.
                    // Envio CFP(CALLproposal) co precio actual

                    //Ao poñelo aqui podo conseguir que si creo nuevos compradores
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("subasta-libros"); //Buscamos a cualquiera que sea de tipo "comprador de libros"
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(agent, template);
                        compradores = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            compradores[i] = result[i].getName();
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // COmpruebo que haya compradores
                    if (compradores.length == 0) {
                        // Si no hay compradores cada 5 ssegundos voy comprobando si alguien se une
                        // Uso block() para no congelar al resto del agente, porque con sleep se bloquearia todo, con block libero cpu

                         
                        myGui.log("[" + libro + "] Sin compradores. Esperando...");
                        block(5000);
                        return;
                    }

                    // 3.Inicio a ronda de subasta Calzoncillos a 5 euros!!
                    
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID comprador : compradores) {
                        cfp.addReceiver(comprador); //añado a todos os compradores pa que lle chegue a notificacions
                    }
                    cfp.setContent(libro + ":" + precioActual); //Contido"Libro:precio"

                    // ID de Conversación para distinguir la subasta del libro A de la del libro B
                    cfp.setConversationId("subasta-" + libro);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // ID único de este mensaje
                    agent.send(cfp); // ¡Enviado!

                    // 4.Filtro mensajes
                    // .Aqui para que o vendedor poida gestionar distintos libros ao mismo tempo teño: ConversationID e MessageTemplate
                    //
                    //Cada vez que se inicia unha subasta (instancia de FuncionamientoSubasta)
                    // poñolle un ID a cada mensaje (subasta-tituloLibro),
                    // despois co MessageTemplate o que fago e que me filtre e solo lea os mensajes con ese ID,
                    // si mezclar pujas do libroA coas do libroB
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("subasta-" + libro),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                    );

                    // Reseteamos contadores para empezar la ronda siguiente
                    respuestasRecibidas = 0;
                    numPropuestas = 0;
                    posibleGanador = null; //null para capturar al "primero" que llegue

                    myGui.addOrUpdateSubasta(libro, precioActual, 0, "Esperando ofertas...");
                    paso = 2; // Avanzamos a la fase de esperar respuestas
                    break;

                case 2: 
                    // Miramos en el buzón usando el filtro (mt) que creamos antes
                    ACLMessage msg = agent.receive(mt);

                    if (msg != null) {
                        // ¡Ha llegado una carta válida!
                        respuestasRecibidas++;

                        // Si el mensaje es PROPOSE, significa que quieren comprar (pujan)
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            numPropuestas++;

                            // Asignase al PRIMER comprador
                            // Si posibleGanador está vacío, este es el primero.
                            if (posibleGanador == null) {
                                posibleGanador = msg.getSender();
                            }
                            // Si ya había alguien, ignoramos a este segundo (aunque contamos su propuesta para saber que hay competencia)

                            myGui.addOrUpdateSubasta(libro, precioActual, numPropuestas, "Oferta recibida");
                        }
                        //  Si mandan REFUSE, solo sumo respuestasRecibidas pero no hacemos nada más.

                        // Si ya han contestado TODOS los compradores que encontramos...
                        if (respuestasRecibidas >= compradores.length) {
                            // REQUISITO: "El vendedor debe esperar 10 segundos antes de asignar nuevo precio"
                            // Guardamos la hora a la que podremos seguir.
                            esperaDecision = System.currentTimeMillis() + 10000;
                            paso = 3; // Nos vamos a la sala de espera
                        }
                    } else {
                        // Si el buzón está vacío, nos bloqueamos hasta que llegue correo nuevo.
                        // Esto libera la CPU para que otras subastas funcionen
                        block();
                    }
                    break;

                case 3: 
                    // Calculamos cuánto falta para que pasen los 10 segundos
                    long tiempoRestante = esperaDecision - System.currentTimeMillis();

                    if (tiempoRestante > 0) {
                        //si ainda non pasan os 10 segundos durmo ao axente o tempo restante
                        block(tiempoRestante);
                        return; // Salimos y volvemos luego
                    }

                    

                    if (numPropuestas == 0) {
                        // CASO 1  Nadie quiere el libro a este precio.
                        if (ganadorAnterior != null) {
                            //si nadie nesta ronda quiere el libro se lo damos al que pujo en la ronda anterior por el
                            
                            int precioVenta = precioActual - incremento;
                            aceptarVenta(ganadorAnterior, precioVenta);
                        } else {
                            //nadie nunca ha pujado por el subasta terminada.
                            myGui.log("[" + libro + "] Nadie pujó por el precio inicial.");
                            notificarFracaso();
                        }
                        terminado = true; // Fin del comportamiento
                    }
                    else if (numPropuestas == 1) {
                        // CASO 2 Solo hay UN interesado en esta ronda.
                        // No hace falta subir más el precio, se lo vendemos a él directamente.
                        
                        aceptarVenta(posibleGanador, precioActual);
                        terminado = true; // Fin
                    }
                    else {
                        // CASO 3 Hay COMPETENCIA (>1 propuestas).
                        // Guardamos al ganador de esta ronda como "ganadorAnterior" por si en la siguiente nadie puja
                        ganadorAnterior = posibleGanador;

                        // Subo el precio 
                        precioActual += incremento;
                        myGui.log("[" + libro + "] ¡Competencia (" + numPropuestas + " pujas)! Subiendo precio a " + precioActual);

                        //Volver al paso 1, volver a buscar compradores y enviar nuevo precio.
                        paso = 1;
                    }
                    break;
            }
        }

        // Método auxiliar para cerrar el trato
        private void aceptarVenta(AID ganador, int precio) {
            // 1. Mandamos mensaje al ganador: "ACCEPT_PROPOSAL"
            ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            order.addReceiver(ganador);
            order.setContent(libro + ":" + precio);
            order.setConversationId("subasta-" + libro);
            agent.send(order);

            // 2. Mandamos mensaje a TODOS (perdedores incluidos) para informar
            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            for(AID c : compradores) {
                if(!c.equals(ganador)) info.addReceiver(c); // A todos menos al que ya le avisé
            }
            //"FINALIZADA:Libro:Ganador:Precio".IMPORTANTE MISMO FORMATO NO COMPRADOR
            info.setContent("FINALIZADA:" + libro + ":" + ganador.getLocalName() + ":" + precio);
            info.setConversationId("subasta-" + libro);
            agent.send(info);

            myGui.log("[" + libro + "] VENDIDO a " + ganador.getLocalName() + " por " + precio);
            myGui.addOrUpdateSubasta(libro, precio, 0, "VENDIDO");
            subastasActivas.remove(libro); // Borramos de la lista de tareas activas
        }

        // Método auxiliar cuando nadie compra
        private void notificarFracaso() {
            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            for(AID c : compradores) info.addReceiver(c);

            // Aviso de que se acabó sin venta
            info.setContent("FINALIZADA-SIN-VENTA:" + libro);
            info.setConversationId("subasta-" + libro);
            agent.send(info);

            myGui.addOrUpdateSubasta(libro, precioActual, 0, "CANCELADA");
            subastasActivas.remove(libro);
        }

        // Método que JADE consulta para saber si debe borrar este comportamiento de la memoria
        public boolean done() {
            return terminado;
        }
    }
    //Terminacion del agente
    protected void takeDown() {
        if (myGui != null) {
            myGui.dispose(); // Cierra la ventana si se cierra el agente
        }
        System.out.println("Vendedor " + getLocalName() + " terminando.");

    }



}

/*Protocolo comunicacion
FIPA-CONTRACT-NET
 1. Vendedor envia un CFP
 2.Compradores mandan PROPOSE ou REFUSE
 3. Vendedor envia ACCEPT_PROPOSAL al gandadore INFORM a los perdedores
 */