package edu.upc.fib.sid;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.tools.sniffer.Sniffer;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SidWwtp {
    private static AgentContainer container;

    public static void main(String[] args) throws StaleProxyException {
        final Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "SIDWWTP2019");
        profile.setParameter(Profile.GUI, "true");
        
        // Local Host
        profile.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
        Profile p = new ProfileImpl();
        p.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
        
        
        final Runtime runtime = Runtime.instance();
        runtime.createMainContainer(p);
        container = runtime.createAgentContainer(profile);

        final AgentController sniffer = container.createNewAgent("Sniffer", Sniffer.class.getName(), null);
        final AgentController environment = container.createNewAgent("Environment", Environment.class.getName(), null);
        final AgentController industry = container.createNewAgent("Industry", Industry.class.getName(), null);
        final AgentController plant = container.createNewAgent("TreatmentPlant", TreatmentPlant.class.getName(), null);

        sniffer.start();
        environment.start();
        industry.start();
        plant.start();
    }
}
