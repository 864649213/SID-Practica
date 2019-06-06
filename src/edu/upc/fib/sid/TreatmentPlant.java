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

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class TreatmentPlant extends Agent {
    private Logger logger = Logger.getLogger("TreatmentPlant");

    protected void setup() {
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
                heldConversations.add(cfp.getConversationId());
                final ACLMessage reply = cfp.createReply();
                if(Math.random() > 0.5) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("(transaction :price 1000)");
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                }
                return reply;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                final ACLMessage reply = accept.createReply();
                if(Math.random() > 0.4) {
                    reply.setPerformative(ACLMessage.INFORM);
                } else if(Math.random() > 0.1) {
                    reply.setPerformative(ACLMessage.FAILURE);
                }
                return reply;
            }
        };
        parallelBehaviour.addSubBehaviour(responder);

        parallelBehaviour.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                double currentConcentration = Math.random();
                double currentAvailability = Math.random() * 750;

                logger.info("TREATMENTPLANT[currentConcentration]: " + currentConcentration);
                logger.info("TREATMENTPLANT[currentAvailability]: " + currentAvailability);

                if(Math.random() > 0.9) {
                    final DFAgentDescription desc = new DFAgentDescription();
                    final ServiceDescription sdesc = new ServiceDescription();
                    sdesc.setType("Environment");
                    desc.addServices(sdesc);
                    try {
                        final DFAgentDescription[] environments = DFService.search(TreatmentPlant.this, getDefaultDF(), desc,
                                new SearchConstraints());
                        final AID environment = environments[0].getName();
                        final ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
                        aclMessage.setSender(TreatmentPlant.this.getAID());
                        aclMessage.addReceiver(environment);
                        aclMessage.setContent("(discharge :volume-water 100 :concentration-pollutant 0.7)");
                        TreatmentPlant.this.send(aclMessage);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        this.addBehaviour(parallelBehaviour);
    }
}
