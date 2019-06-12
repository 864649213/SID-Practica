package edu.upc.fib.sid;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import java.net.URISyntaxException;
import java.util.logging.Level;

import java.util.logging.Logger;

public class Environment extends Agent {
    private Logger logger = Logger.getLogger("Environment");
    
    // Volumen en m3
    float volume;
    // Concentración %
    float concentration;
    // Volumen que entra por cada unidad de tiempo
    float volumeIn;
    float concentrationIn;
    // Volumen que sale por cada unidad de tiempo
    float volumeOut;
    // Probabilidad de sancion %
    float sanctionProbability;
    // Unidad de precio de sancion por tonelada
    int sanctionPerTonDischarged;
    
    private WwtpDomain domini;

    protected void setup() {
        
        try {
            domini = OntologyParser.parse();
            volumeIn = domini.getWaterReceivedVolume();
            concentrationIn = domini.getWaterReceivedSolidsConcentration();       
            volumeOut = domini.getReleasedWaterPerTimeUnit();
            sanctionProbability = domini.getChanceOfDetectingIllegalDischarge();
            sanctionPerTonDischarged = domini.getSanctionPerTonDischarged();
            
        } catch (URISyntaxException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
            volumeIn = 2000;
            concentrationIn = 5;
            volumeOut = 2000;
            sanctionProbability = 30;
            sanctionPerTonDischarged = 40;
        }
        
        volume = 8000;
        concentration = 5;

        
        final DFAgentDescription desc = new DFAgentDescription();
        desc.setName(getAID());

        final ServiceDescription sdesc = new ServiceDescription();
        sdesc.setName("Environment");
        sdesc.setType("Environment");
        desc.addServices(sdesc);

        try {
            DFService.register(this, getDefaultDF(), desc);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        final ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);

        parallelBehaviour.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                logger.info("ENVIRONMENT[currentVolume]: " + volume);
                logger.info("ENVIRONMENT[currentConcentration]: " + concentration);
                
                float actualMass = volume*concentration;
                float incomingMass = volumeIn*concentrationIn;
                volume += volumeIn;
                actualMass += incomingMass;
                if (volume > 0) concentration = actualMass/volume;
                else concentration = 0;

                volume -= volumeOut;
               
            }
        });

        final MessageTemplate mt = new MessageTemplate((MessageTemplate.MatchExpression) aclMessage -> true);

        parallelBehaviour.addSubBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                final ACLMessage request = Environment.this.blockingReceive(mt);
                final AID sender = request.getSender();
                final ACLMessage reply = request.createReply();
                try {
                    final DFAgentDescription desc = new DFAgentDescription();
                    desc.setName(sender);
                    final DFAgentDescription[] search = DFService.search(Environment.this, getDefaultDF(), desc);
                    final Iterator services = search[0].getAllServices();
                    final ServiceDescription service = (ServiceDescription) services.next();
                    // Industria 
                    if (service.getType().equals("Industry")) {
                        // recibir mensaje discharge de industria
                        String str = request.getContent();
                        str = str.replace("(", "").replace(")", "");
                        String[] splitted = str.split("\\s+");
                        float volumeIn = Float.parseFloat(splitted[2]);
                        float concentrationIn = Float.parseFloat(splitted[4]);
                        float massIn = volumeIn*concentrationIn;
                        volume += volumeIn;
                        float finalMass = volume*concentration+massIn;
                        if (volume > 0) concentration = finalMass/volume;
                        else concentration = 0;
                        
                        float sanctionProbability = OntologyParser.parse().getChanceOfDetectingIllegalDischarge();
                        float r = ((float)Math.random()*100)%100;                       
                        // Descarga ilegal pillada
                        if (r <= sanctionProbability) {
                            float sanctionPrice = sanctionPerTonDischarged * massIn;
                            String msg = "(sanction :cost " + sanctionPrice + ")";
                            reply.setPerformative(ACLMessage.REQUEST);
                            reply.setContent(msg);
                        }
                        // No pillado
                        else {
                            reply.setPerformative(ACLMessage.INFORM);
                        }
                    } 
                    // Planta
                    else {
                        String str = request.getContent();
                        str = str.replace("(", "").replace(")", "");
                        String[] splitted = str.split("\\s+");
                        float volumeIn = Float.parseFloat(splitted[2]);
                        float concentrationIn = Float.parseFloat(splitted[4]);
                        float massIn = volumeIn*concentrationIn;
                        volume += volumeIn;
                        float finalMass = volume*concentration+massIn;
                        if (volume > 0) concentration = finalMass/volume;
                        else concentration = 0;
                        reply.setPerformative(ACLMessage.INFORM);
                    }
                    Environment.this.send(reply);
                    
                } catch (FIPAException e) {
                    e.printStackTrace();
                    
                } catch (URISyntaxException ex) {
                    Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        this.addBehaviour(parallelBehaviour);
    }
}
