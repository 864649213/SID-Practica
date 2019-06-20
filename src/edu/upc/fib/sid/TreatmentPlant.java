package edu.upc.fib.sid;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import java.net.URISyntaxException;
import java.util.Date;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class TreatmentPlant extends Agent {
    private Logger logger = Logger.getLogger("TreatmentPlant");
    
    float maxVolume;    // in m3
    float actVolume;
    float actMass;  // in tons
    float costPerMCubicWater;
    float costPerTonsSolid;
    float profit;
    float profitFactor = (float)1.1;
    
    private WwtpDomain domini;

    protected void setup() {
        try {
            domini = OntologyParser.parse();
            maxVolume = domini.getPlantStorageAvailability();
            costPerMCubicWater = domini.getCostPerCubicMeterTreated();
            costPerTonsSolid = domini.getCostPerTonOfPollutantTreated();
        } catch (URISyntaxException ex) {
            maxVolume = 750;
            costPerMCubicWater = 1;
            costPerTonsSolid = 1;
        }
        actVolume = 0;
        actMass = 0;
        profit = 0;
        
        
        final DFAgentDescription desc = new DFAgentDescription();
        desc.setName(getAID());

        final ServiceDescription sdesc = new ServiceDescription();
        sdesc.setName("TreatmentPlant");
        sdesc.setType("TreatmentPlant");
        desc.addServices(sdesc);

        try {
            DFService.register(this, getDefaultDF(), desc);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        

        final ParallelBehaviour parallelBehaviour = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        
        parallelBehaviour.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                float currentConcentration;
                if (actVolume == 0) currentConcentration = 0;
                else currentConcentration = actMass/actVolume;

                logger.info("TREATMENTPLANT[currentConcentration]: " + currentConcentration);
                logger.info("TREATMENTPLANT[storageOccupied]: " + actVolume);
                logger.info("TREATMENTPLANT[maximumStorage]: " + domini.getPlantStorageAvailability());
                logger.info("TREATMENTPLANT[profit]: " + profit);

                dischargeToEnvironment(actVolume, currentConcentration);
                actVolume = 0;
                //if (Math.random() < 0.5) actVolume = 750;
                actMass = 0;
                
            }
        });
        

        final Set<String> heldConversations = new TreeSet<>();
        final ContractNetResponder responder = new ContractNetResponder(this, new MessageTemplate(
                (MessageTemplate.MatchExpression) aclMessage -> {
                    if(aclMessage.getPerformative() == ACLMessage.CFP) {
                        if(aclMessage.getConversationId() == null) {
                            return true;
                        } else {
                            return !heldConversations.contains(aclMessage.getConversationId());
                        }
                    } else {
                        return false;
                    }
                })) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                //System.out.println("Planta: " + new Date());
                heldConversations.add(cfp.getConversationId());
                String str = cfp.getContent();
                str = str.replace("(", "").replace(")", "");
                String[] splitted = str.split("\\s+");
                float volumeIn = Float.parseFloat(splitted[2]);
                float concentrationIn = Float.parseFloat(splitted[4]);
                float massIn = volumeIn * concentrationIn;
                
                final ACLMessage reply = cfp.createReply();
                
                // aun hay espacio
                if(actVolume + volumeIn <= maxVolume) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    float cost = volumeIn*costPerMCubicWater + massIn*costPerTonsSolid;
                    float price = profitFactor*cost; // beneficio
                    String msg = "(transaction :price " + price + ")";
                    reply.setContent(msg);
                } 
                else {  // no hay espacio, verter agua con contaminacion al rio, subir precio o rechazo
                    if (Math.random() <= 0.5) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        
                        float volumeToStore = maxVolume-actVolume;
                        float massToStore = volumeToStore*concentrationIn;
                        float volumeToEnvironment = volumeIn-volumeToStore;
                        float massToEnvironment = volumeToEnvironment*concentrationIn;
                        
                        float normalCost = volumeToStore*costPerMCubicWater + massToStore*costPerTonsSolid;
                        float extraCost = volumeToEnvironment*costPerMCubicWater + massToEnvironment*costPerTonsSolid;
                        float price = profitFactor*(normalCost + 2*extraCost); 
                        String msg = "(transaction :price " + price + ")";
                        reply.setContent(msg);
                    }
                    
                    else reply.setPerformative(ACLMessage.REFUSE);
                }
                return reply;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                final ACLMessage reply = accept.createReply();  
                String str = cfp.getContent();
                str = str.replace("(", "").replace(")", "");
                String[] splitted = str.split("\\s+");
                float volumeIn = Float.parseFloat(splitted[2]);
                float concentrationIn = Float.parseFloat(splitted[4]);
                float massIn = volumeIn * concentrationIn;
                if(actVolume + volumeIn > maxVolume) { // al rio directamente      
                    float volumeToStore = maxVolume-actVolume;
                    float massToStore = volumeToStore*concentrationIn;
                    float volumeToEnvironment = volumeIn-volumeToStore;
                    float massToEnvironment = volumeToEnvironment*concentrationIn;
                    float concentrationToEnvironment = massToEnvironment/volumeToEnvironment;
                    float normalProfit = volumeToStore*costPerMCubicWater + massToStore*costPerTonsSolid;
                    float extraProfit = volumeToEnvironment*costPerMCubicWater + massToEnvironment*costPerTonsSolid;
                    
                    actVolume += volumeToStore;
                    actMass += massToStore;
                    profit += normalProfit + extraProfit;
                         
                    dischargeToEnvironment(volumeToEnvironment, concentrationToEnvironment);
                }
                else {  // guardarlo para enviar al siguiente tick
                    actVolume += volumeIn;
                    actMass += massIn; 
                    profit += profitFactor*(volumeIn*costPerMCubicWater + massIn*costPerTonsSolid);
                }
                
                reply.setPerformative(ACLMessage.INFORM);
                return reply;
            }
        };
        parallelBehaviour.addSubBehaviour(responder);

        this.addBehaviour(parallelBehaviour);
    }
    
    public void dischargeToEnvironment(float volume, float concentration) {
        final DFAgentDescription desc = new DFAgentDescription();
        final ServiceDescription sdesc = new ServiceDescription();
        sdesc.setType("Environment");
        desc.addServices(sdesc);
        try {
            final DFAgentDescription[] environments = DFService.search(TreatmentPlant.this, getDefaultDF(), desc, new SearchConstraints());
            final AID environment = environments[0].getName();
            final ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
            aclMessage.setSender(TreatmentPlant.this.getAID());
            aclMessage.addReceiver(environment);
            aclMessage.setContent("(discharge :volume-water " + volume +" :concentration-pollutant " + concentration +")");
            TreatmentPlant.this.send(aclMessage);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}