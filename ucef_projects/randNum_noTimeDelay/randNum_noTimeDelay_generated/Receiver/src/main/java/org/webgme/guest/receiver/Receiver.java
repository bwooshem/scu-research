package org.webgme.guest.receiver;

import org.webgme.guest.receiver.rti.*;

import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateConfigParser;
import org.cpswt.hla.InteractionRoot;
import org.cpswt.hla.base.AdvanceTimeRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.utils.CpswtUtils;

// Define the Receiver type of federate for the federation.

public class Receiver extends ReceiverBase {
    private final static Logger log = LogManager.getLogger();

    private double currentTime = 0;

    public Receiver(FederateConfig params) throws Exception {
        super(params);
    }

    boolean receivedSimTime = false;
    double rand1 = -1;
    double rand2 = -1;
    double rand3 = -1;
    int time1 = -1;
    int time2 = -1;
    int time3 = -1;
    
    private void checkReceivedSubscriptions() {
        InteractionRoot interaction = null;
        while ((interaction = getNextInteractionNoWait()) != null) {
            if (interaction instanceof NumGen_Receiver) {
                handleInteractionClass((NumGen_Receiver) interaction);
            }
            else if (interaction instanceof Simtime) {
                handleInteractionClass((Simtime) interaction);
            }
            else {
                log.debug("unhandled interaction: {}", interaction.getClassName());
            }
        }
    }

    private void execute() throws Exception {
        if(super.isLateJoiner()) {
            log.info("turning off time regulation (late joiner)");
            currentTime = super.getLBTS() - super.getLookAhead();
            super.disableTimeRegulation();
        }

        /////////////////////////////////////////////
        // TODO perform basic initialization below //
        /////////////////////////////////////////////

        AdvanceTimeRequest atr = new AdvanceTimeRequest(currentTime);
        putAdvanceTimeRequest(atr);

        if(!super.isLateJoiner()) {
            log.info("waiting on readyToPopulate...");
            readyToPopulate();
            log.info("...synchronized on readyToPopulate");
        }

        

        ///////////////////////////////////////////////////////////////////////
        // TODO perform initialization that depends on other federates below //
        ///////////////////////////////////////////////////////////////////////

        if(!super.isLateJoiner()) {
            log.info("waiting on readyToRun...");
            readyToRun();
            log.info("...synchronized on readyToRun");
        }

        startAdvanceTimeThread();
        log.info("started logical time progression");

        

        while (!exitCondition) {
            atr.requestSyncStart();
            enteredTimeGrantedState();

            // removing time delay...
            receivedSimTime = false;
            while (!receivedSimTime){
                log.info("waiting to receive SimTime...");
                synchronized(lrc){
                    lrc.tick();
                }
                checkReceivedSubscriptions();
                if(!receivedSimTime){
                    CpswtUtils.sleep(1000);
                }
            }
            //

            ////////////////////////////////////////////////////////////
            // TODO send interactions that must be sent every logical //
            // time step below                                        //
            ////////////////////////////////////////////////////////////

            double sum = 0;
            sum = rand1 + rand2 + rand3;
            log.info("rec sum: ",sum);
            System.out.println("rec sum: "+sum);
            int timestep = (int)currentTime;
            log.info("rec timestep: ",currentTime);
            System.out.println("timestep: "+currentTime);
            log.info("time1: ",time1);
            System.out.println("time1: "+time1);
            log.info("time2: ",time2);
            System.out.println("time2: "+time2);
            log.info("time3: ",time3);
            System.out.println("time3: "+time3);

            Receiver_NumGen vReceiver_NumGen = create_Receiver_NumGen();
            vReceiver_NumGen.set_sum(sum);
            vReceiver_NumGen.set_timestep(timestep);
            vReceiver_NumGen.sendInteraction(getLRC());

            rand1 = -1;
            rand2 = -1;
            rand3 = -1;

            // Set the interaction's parameters.
            //
            //    Receiver_NumGen vReceiver_NumGen = create_Receiver_NumGen();
            //    vReceiver_NumGen.set_actualLogicalGenerationTime( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_dataString( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_federateFilter( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_originFed( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_sourceFed( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_sum( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.set_timestep( < YOUR VALUE HERE > );
            //    vReceiver_NumGen.sendInteraction(getLRC(), currentTime + getLookAhead());

            checkReceivedSubscriptions();

            ////////////////////////////////////////////////////////////////////
            // TODO break here if ready to resign and break out of while loop //
            ////////////////////////////////////////////////////////////////////

            if (!exitCondition) {
                currentTime += super.getStepSize();
                AdvanceTimeRequest newATR =
                    new AdvanceTimeRequest(currentTime);
                putAdvanceTimeRequest(newATR);
                atr.requestSyncEnd();
                atr = newATR;
            }
        }

        // call exitGracefully to shut down federate
        exitGracefully();

        //////////////////////////////////////////////////////////////////////
        // TODO Perform whatever cleanups are needed before exiting the app //
        //////////////////////////////////////////////////////////////////////
    }

    private void handleInteractionClass(NumGen_Receiver interaction) {
        ///////////////////////////////////////////////////////////////
        // TODO implement how to handle reception of the interaction //
        ///////////////////////////////////////////////////////////////

        if(rand1 == -1){
            rand1= interaction.get_randNum();
            time1 = interaction.get_timestep();
        }
        else if(rand2 ==-1){
            rand2 = interaction.get_randNum();
            time2 = interaction.get_timestep();
        }
        else if(rand3 ==-1){
            rand3 = interaction.get_randNum();
            time3 = interaction.get_timestep();
        }
    }

    private void handleInteractionClass(Simtime interaction) {
        ///////////////////////////////////////////////////////////////
        // TODO implement how to handle reception of the interaction //
        ///////////////////////////////////////////////////////////////
        receivedSimTime = true;
    }

    public static void main(String[] args) {
        try {
            FederateConfigParser federateConfigParser =
                new FederateConfigParser();
            FederateConfig federateConfig =
                federateConfigParser.parseArgs(args, FederateConfig.class);
            Receiver federate =
                new Receiver(federateConfig);
            federate.execute();
            log.info("Done.");
            System.exit(0);
        }
        catch (Exception e) {
            log.error(e);
            System.exit(1);
        }
    }
}
