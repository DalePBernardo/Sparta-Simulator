package com.spartaglobal.spartasimulator;

import java.util.Arrays;
import java.util.Random;

public class Simulation {

    private static final int MIN_GENERATED_TRAINEES = 50;
    private static final int MAX_GENERATED_TRAINEES = 100;
    private static final double CLIENT_CREATION_CHANCE = 0.5;
    private static final Random rand = new Random();

    public static void simulate(int months, boolean infoGivenMonthly, TraineeDAO tdao){
        TraineeFactory tf = new TraineeFactory();
        TrainingCentreFactory tcf = new TrainingCentreFactory();
        ClientFactory cf = new ClientFactory();
        for(int i = 0; i < months; i++) {
            loop(i, tdao, tf, tcf, cf);
            if(infoGivenMonthly) DisplayManager.printSystemInfo(tdao);
        }
        if(!infoGivenMonthly) DisplayManager.printSystemInfo(tdao);
    }

    private static void loop(int month, TraineeDAO tdao, TraineeFactory tf, TrainingCentreFactory tcf, ClientFactory cf){
        if((month % 2) == 1) {
            TrainingCentre centre = tcf.makeCentre();
            tdao.addTrainingCentre(centre);
            if(centre.getCentreType().equals("Boot Camp")) for(int i = 0; i < 2; i++) tdao.addTrainingCentre(tcf.makeCentre("Boot Camp"));
        }

        // for all happy clients that have been waiting for over a year, create a new requirement
        Arrays.stream(tdao.getHappyClients()).forEach(c -> tdao.addRequirement(c)).forEach(c -> tdao.setClientWaiting(c));

        if((month >= 12) && (rand.nextDouble() < CLIENT_CREATION_CHANCE)) tdao.addClient(cf.makeClient());

        // trainees that have been training for a year become benched
        Arrays.stream(tdao.getTraineesTrainingOverAYear()).forEach(t -> tdao.setTraineeBenched(t));
        // get benched trainees
        Trainee[] benchedTrainees = tdao.getBenchedTrainees();
        // get waiting clients ordered by wait time
        Client[] waitingClients = tdao.getWaitingClients();
        // for each benched trainee, try to assign to a client, with priority to clients earlier in list
        Arrays.stream(benchedTrainees).forEach(t -> tdao.assignTraineeToRequirement(t, waitingClients));
        // set clients with requirements met to "happy"
        tdao.setSatisfiedClientsToHappy();
        // set clients that have been waiting for over a year and have not met their requirements to "unhappy"
        // and bench any trainees assigned to their most recent requirement
        Client[] unsatisfiedClients = tdao.getUnsatisfiedClients();
        for(Client c : unsatisfiedClients) {
            tdao.setClientUnhappy(c);
            for(Trainee t : getWorkingTrainees(c)) {
                tdao.setTraineeBenched(t);
            }
        }

        Arrays.stream(tdao.getWaitingTrainees(true)).forEach(t -> tdao.addTrainee(t));
        Arrays.stream(tf.getNewTrainees(MIN_GENERATED_TRAINEES, MAX_GENERATED_TRAINEES)).forEach(t -> tdao.addTrainee(t));

        // potentially close centres and redistribute trainees
        Arrays.stream(tdao.getLowAttendanceCentres()).forEach(tc -> tdao.closeCentre(tc));
        tdao.reassignTraineesInClosedCentres();
    }
}
