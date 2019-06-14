package edu.upc.fib.sid;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.util.FileManager;

import java.io.InputStream;
import java.net.URISyntaxException;

public class OntologyParser {
    public static WwtpDomain parse() throws URISyntaxException {
        WwtpDomain domain = new WwtpDomain();
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream inputStream =
                FileManager.get().open(String.valueOf(OntologyParser.class.getClassLoader()
                        .getResource("edu/upc/fib/sid/sid2019-wwtp-final.owl").toURI()));
        model.read(inputStream, null);

        Individual factory = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#Industry-1");
        Individual riverSegment = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#CentralRiverSegment");
        Individual sanctioningPolicy = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#BesosRiverSanctioningPolicy");
        Individual waterReceived = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#WaterReceived");
        Individual solidsConcentration = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#WaterReceivedSolidsConcentration");
        Individual plantaBesos = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#PlantaBesos");
        Individual suspendedSolidsTreated = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#PlantaBesosSuspendedSolidsTreated");
        Individual sidBesosRiver2019 = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#SIDBesosRiver2019");
        Individual foodSuspendedSolidsGeneration = model.getIndividual("http://fib.upc.edu/sid/2019/wwtp#FoodSuspendedSolidsGeneration");

        Property maximumProduction = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#maximumProduction");
        Property profitPerTonProduced = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#profitPerTonProduced");
        Property storageAvailability = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#storageAvailability");
        Property wastePerProduction = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#wastePerProduction");
        Property chanceOfDetectingIllegalDischarge = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#hasChanceOfDetectingIllegalDischarge");
        Property sanctionPerTonDischarged = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#sanctionPerTonDischarged");
        Property releasedWaterPerTimeUnit = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#releasedWaterPerTimeUnit");
        Property hasVolume = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#hasVolume");
        Property hasValue = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#hasValue");
        Property costPerCubicMeterTreated = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#costPerCubicMeterTreated");
        Property costPerTonOfPollutantTreated = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#costPerTonOfPollutantTreated");
        Property hasTimeUnit = model.getProperty("http://fib.upc.edu/sid/2019/wwtp#hasTimeUnit");

        domain.setMaximumProduction(factory.getPropertyValue(maximumProduction).asLiteral().getFloat());
        domain.setProfitPerTonProduced(factory.getPropertyValue(profitPerTonProduced).asLiteral().getInt());
        domain.setIndustryStorageAvailability(factory.getPropertyValue(storageAvailability).asLiteral().getFloat());
        domain.setWastePerProduction(factory.getPropertyValue(wastePerProduction).asLiteral().getFloat());
        domain.setChanceOfDetectingIllegalDischarge(sanctioningPolicy.getPropertyValue(chanceOfDetectingIllegalDischarge).asLiteral().getFloat());
        domain.setSanctionPerTonDischarged(sanctioningPolicy.getPropertyValue(sanctionPerTonDischarged).asLiteral().getInt());
        domain.setReleasedWaterPerTimeUnit(riverSegment.getPropertyValue(releasedWaterPerTimeUnit).asLiteral().getFloat());
        domain.setWaterReceivedVolume(waterReceived.getPropertyValue(hasVolume).asLiteral().getFloat());
        domain.setWaterReceivedSolidsConcentration(solidsConcentration.getPropertyValue(hasValue).asLiteral().getFloat());
        domain.setSuspendedSolidsTreated(suspendedSolidsTreated.getPropertyValue(hasValue).asLiteral().getFloat());
        domain.setCostPerCubicMeterTreated(plantaBesos.getPropertyValue(costPerCubicMeterTreated).asLiteral().getInt());
        domain.setCostPerTonOfPollutantTreated(plantaBesos.getPropertyValue(costPerTonOfPollutantTreated).asLiteral().getInt());
        domain.setPlantStorageAvailability(plantaBesos.getPropertyValue(storageAvailability).asLiteral().getFloat());
        domain.setTimeUnit(sidBesosRiver2019.getPropertyValue(hasTimeUnit).asLiteral().getInt());
        
        domain.setPollutantGenerated(foodSuspendedSolidsGeneration.getPropertyValue(hasValue).asLiteral().getFloat());

        return domain;
    }

    public static void main(String[] args) throws URISyntaxException {
        WwtpDomain domain = OntologyParser.parse();
        System.out.println(domain);
    }
}
