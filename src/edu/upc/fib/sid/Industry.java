package edu.upc.fib.sid;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.net.URISyntaxException;

import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Industry extends Agent {
    private float cumulativeProfit;
    private float storageOccupied; //Cantidad ocupada del deposito de la industria. metros cubicos (m3)
    private float dischargeTick;
    private WwtpDomain domini;
    
    protected void setup() {
        
        cumulativeProfit = 0; //Empezamos habiendo ganado 0 unidades monetarias.
        storageOccupied = 0; //Empezamos con el deposito vacio
        
        try {
            domini = OntologyParser.parse();
        } catch (URISyntaxException ex) {
            Logger.getLogger(Industry.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        final DFAgentDescription desc = new DFAgentDescription();
        desc.setName(getAID());

        final ServiceDescription sdesc = new ServiceDescription();
        sdesc.setName("Industry");
        sdesc.setType("Industry");
        desc.addServices(sdesc);

        try {
            DFService.register(this, getDefaultDF(), desc);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        this.addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {

                // Copy these two lines in your industry agent replacing 1 with your corresponding group id
                Logger.getLogger("Industry-4").info("INDUSTRY[cumulativeProfit]: " + cumulativeProfit);
                Logger.getLogger("Industry-4").info("INDUSTRY[maximumStorage]: " + domini.getIndustryStorageAvailability());
                Logger.getLogger("Industry-4").info("INDUSTRY[storageOccupied]: " + storageOccupied);
                //

                /*if(Math.random() > 0.8) {
                    final DFAgentDescription desc = new DFAgentDescription();
                    final ServiceDescription sdesc = new ServiceDescription();
                    sdesc.setType("Environment");
                    desc.addServices(sdesc);
                    try {
                        final DFAgentDescription[] environments = DFService.search(Industry.this, getDefaultDF(), desc,
                                new SearchConstraints());
                        final AID environment = environments[0].getName();
                        final ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
                        aclMessage.setSender(Industry.this.getAID());
                        aclMessage.addReceiver(environment);
                        aclMessage.setContent("(discharge :volume-water 200 :concentration-pollutant 0.5)");
                        Industry.this.send(aclMessage);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                } else if(Math.random() > 0.5) {
                    final DFAgentDescription desc = new DFAgentDescription();
                    final ServiceDescription sdesc = new ServiceDescription();
                    sdesc.setType("TreatmentPlant");
                    desc.addServices(sdesc);
                    try {
                        final DFAgentDescription[] plants = DFService.search(Industry.this, getDefaultDF(), desc,
                                new SearchConstraints());
                        final AID plant = plants[0].getName();
                        final ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        cfp.setSender(Industry.this.getAID());
                        cfp.addReceiver(plant);
                        cfp.setContent("(discharge :volume-water 300 :concentration-pollutant 0.3)");
                        cfp.setReplyByDate(new Date(System.currentTimeMillis() + 2000));
                        Industry.this.addBehaviour(new ContractNetInitiator(Industry.this, cfp) {
                            @Override
                            protected void handleAllResponses(Vector responses, Vector acceptances) {
                                for(Object response: responses) {
                                    final ACLMessage message = (ACLMessage) response;
                                    final ACLMessage reply = message.createReply();
                                    if(Math.random() > 0.5) {
                                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    } else {
                                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    }
                                    acceptances.add(reply);
                                }
                            }
                        });
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }*/
                final DFAgentDescription desc = new DFAgentDescription();
                final ServiceDescription sdesc = new ServiceDescription();
                sdesc.setType("TreatmentPlant");
                desc.addServices(sdesc);
                try {
                    final DFAgentDescription[] plants = DFService.search(Industry.this, getDefaultDF(), desc,
                            new SearchConstraints());
                    final AID plant = plants[0].getName();
                    final ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.setSender(Industry.this.getAID());
                    cfp.addReceiver(plant);
                    dischargeTick = domini.getMaximumProduction() * domini.getWastePerProduction();
                    cfp.setContent("(discharge :volume-water " + dischargeTick + " :concentration-pollutant 0.3)");
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 2000));
                    Industry.this.addBehaviour(new ContractNetInitiator(Industry.this, cfp) {
                        @Override
                        protected void handleAllResponses(Vector responses, Vector acceptances) {
                            for(Object response: responses) {
                                ACLMessage msg = (ACLMessage) response;
                                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                    ACLMessage reply = msg.createReply();
                                    String[] args = msg.getContent().split((" "));
                                    String[] priceaux = args[2].split("\\)");
                                    float price = Float.parseFloat(priceaux[0]); //Precio propuesto por la planta de tratamiento
                                    System.out.println("El precio sugerido por la planta de tratamiento es de: " + price);
                                    System.out.println("Mi beneficio seria de: " + (domini.getMaximumProduction() * domini.getProfitPerTonProduced() - price));
                                    //Comportamiento basico. Si gano dinero acepto, sino rechazo
                                    if (domini.getMaximumProduction() * domini.getProfitPerTonProduced() > price) {
                                            System.out.println(getLocalName() + " - Accepting proposal from sender '"+ msg.getSender().getName() + "'");
                                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                            cumulativeProfit += domini.getMaximumProduction() * domini.getProfitPerTonProduced() - price;
                                            // HAY QUE ENVIAR UN REQUEST A LA PLANTA PARA QUE SEPA QUE ESTAMOS DESCARGANDO O CON EL ACCEPT YA BASTA???????????????????????????????????????????????????????????????????????????????
                                    }
                                    else {
                                        System.out.println(getLocalName() + " - Rejecting proposal from sender '"+ msg.getSender().getName() + "'");
                                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    }
                                    acceptances.add(reply);
                                }
                                else if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                                    Logger.getLogger("Industry-4").info("Propose has been rejected");
                                }
                            }
                        }
                    });
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
