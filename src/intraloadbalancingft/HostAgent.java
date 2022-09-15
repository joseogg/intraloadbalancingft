package intraloadbalancingft;


import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeInitiator;
import jade.proto.ProposeResponder;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author JGUTIERRGARC
 */
public class HostAgent extends Agent {

    private ArrayList<HostDescription> hosts;

    private HostDescription hostDescription;
    private int conversationId;

    private boolean failed;
    private long failureTicksDuration;
    private double failureProbability;

    private double[] lastCPUUsages;
    private double[] lastMemoryUsages;

    private ArrayList<String> coalitionLeaders;
    private Map<String, ArrayList<String>> coalitionToHostAgents; // coalition id, members

    private ArrayList<HostDescription> internalHostsInformation; //  list of hostDescritpions belonging to the current ledear agent's coalition
    private Map<String, ArrayList<HostDescription>> dataCenterHostsInformation; // coalition id, list of hosts' descriptions
    private Map<String, ArrayList<FailureRecord>> hostsFailures; // host agent id, lists of Failures
    private Map<String, Map<String, ArrayList<FailureRecord>>> dataCenterFailures; // coalition id, [host agent id, lists of Failures]

    private Map<String, String> hostAgentToCoalition; // member id to coalition

    private int thresholdViolationCounterForHighCPU;  // This is counting how many times thresholds of high cpu have been violated, the in/decreasing rate depends on the SMA's report frequency
    private int thresholdViolationCounterForHighMemory;  // This is counting how many times thresholds of high memory have been violated, the in/decreasing rate depends on the SMA's report frequency
    private int thresholdViolationCounterForLowCPU;  // This is counting how many times thresholds of low cpu have been violated, the in/decreasing rate depends on the SMA's report frequency
    private int thresholdViolationCounterForLowMemory;  // This is counting how many times thresholds of low memory have been violated, the in/decreasing rate depends on the SMA's report frequency

    private boolean highCPUThresholdViolated;       // I DO NOT USE THEM. However, they might be used to include additional information.
    private boolean highMemoryThresholdViolated;
    private boolean lowCPUThresholdViolated;
    private boolean lowMemoryThresholdViolated;

    private Object logisticRegressionModel;

    private HashSet<weightEdge> edges;
    private Utilities utils;
    transient protected HostAgentGUI hostAgentGUI; // Reference to the gui

    private int currentTick; // this is to keep track of the time window when Consts.MIGRATION_TRIGGER_TYPE == Consts.MIGRATION_TRIGGER_BASED_ON_AVERAGE_USAGE

    private static ExperimentRunConfiguration configuration;

    private WeibullFailureGeneration failureGeneration;
    private int lifeProgress;

    // Frequency of data center server replacement worldwide 2018-2020
    // As per data from a recent report, in 2020, 42 percent of respondents
    // mentioned that they refreshed their data center servers every two to/
    // three years, whilst 26 percent stated that they did so every year.
    // https://www.statista.com/statistics/1109492/frequency-of-data-center-system-refresh-replacement-worldwide/

    public HostAgent() {
        logisticRegressionModel = new Object();
        coalitionToHostAgents = new HashMap<String, ArrayList<String>>();

        failureGeneration = new WeibullFailureGeneration();
        lifeProgress = 0;

        dataCenterFailures = new HashMap<String, Map<String, ArrayList<FailureRecord>>>();
        hostsFailures = new HashMap<String, ArrayList<FailureRecord>>();
        dataCenterHostsInformation = new HashMap<String, ArrayList<HostDescription>>();
        internalHostsInformation = new ArrayList<HostDescription>();


        hostAgentToCoalition = new HashMap<String, String>();
        failed = false;
        failureTicksDuration = 0;
        failureProbability = 0.0; // TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD TBD
        lastCPUUsages = new double[Consts.TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE];
        lastMemoryUsages = new double[Consts.TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE];
        hostDescription = new HostDescription();
        hostDescription.setInProgress(false);
        hostDescription.setFailed(false);
        utils = new Utilities();
        coalitionLeaders = new ArrayList<String>();
        edges = new HashSet<weightEdge>();
        resetAverageUsages();
        resetCounters();
        resetThresholdFlags();
        conversationId = 0;
    }


    @Override
    protected void setup() {
        try {

            Object[] args = getArguments();
            hostDescription = (HostDescription) args[0];
            hostDescription.setContainerName(getContainerController().getContainerName());
            coalitionLeaders = (ArrayList<String>) args[1];
            edges = (HashSet) (args[2]);
            configuration = (ExperimentRunConfiguration) args[3];
            failureProbability = (double) args[4];
            coalitionToHostAgents = getCoalitions((ArrayList<HashSet<String>>) args[5]);

            // Creating a new dictionary including the relationship between hostAgents and coalitions.
            for (String coalitionId : coalitionToHostAgents.keySet()) {
                for (String hostAgent : coalitionToHostAgents.get(coalitionId)) {
                    hostAgentToCoalition.put(hostAgent, coalitionId);
                }
            }

            hostDescription.setMyLeader("HostAgent" + hostDescription.getCoalition());

            if (!Consts.LOG) {
                System.out.println(this.getLocalName() + "'s container is " + this.getContainerController().getContainerName());
            }
            hostAgentGUI = new HostAgentGUI(hostDescription);
            hostAgentGUI.setTitle(getLocalName());
            hostAgentGUI.updateResourceConsumption();
            if (Consts.HOST_AGENT_GUI) {
                hostAgentGUI.setVisible(true);
            }

            if (configuration.getLOAD_BALANCING_TYPE() == Consts.DISTRIBUTED_FIXED_COALITIONS) {
                if (hostDescription.isLeader()) {
                    addBehaviour(new MonitorHAListener(this));
                }

//                addBehaviour(new CNPParticipantForIntraLoadBalancingAtoB(this)); // the agent always listens for potential requests for Intra Load Balancing from A (this source host) to B (a destination host).
                addBehaviour(new ParticipantForIntraLoadBalancingAtoB(this)); // the agent always listens for potential requests for Intra Load Balancing from A (this source host) to B (a destination host).
                addBehaviour(new CNPParticipantForIntraLoadBalancingBtoA(this)); // the agent always listens for potential requests for Intra Load Balancing from B (an external host) to A (this destination host).
                addBehaviour(new PerformanceReporter(this, Consts.HOST_REPORTING_RATE));
                if (configuration.isBALANCING_ONLY_ONE_COALITION_AT_A_TIME()) {
                    if (hostDescription.isLeader()) addBehaviour(new LeaderListenerForCounterReset(this));
                    else addBehaviour(new MemberListenerForCounterReset(this));
                }
            } else if (configuration.getLOAD_BALANCING_TYPE() == Consts.VMWARE_CENTRALIZED_WITH_NO_COALITIONS) {
                addBehaviour(new VMWARE_RemoveAndMigrateVM(this));
                addBehaviour(new VMWARE_LockVM(this));
                addBehaviour(new VMWARE_LockResources(this));
                addBehaviour(new VMWARE_Unlock(this));
                addBehaviour(new VMWARE_ListenerForVMMigrations(this));
                // Add Host Agent's behaviours for centralized load balancing with no coalitions if any.
            }

            // Behaviours required for any balancing type.
            addBehaviour(new ListenerForVMMigrations(this));
            addBehaviour(new RequestsReceiver(this));
            addBehaviour(new PerformanceReporterAndThresholdMonitoring(this, Consts.HOST_REPORTING_RATE + new Random().nextInt((int) Consts.RANGE_OF_RANDOM_TICKS))); // Added a random number to prevent colitions among host agents when enacting interactions protocols.
            addBehaviour(new VirtualMachineKiller(this, (long) (Consts.AVG_INTERDEPARTURE_TIME * (-Math.log(Math.random())))));
            addBehaviour(new MonitorVMListener(this));

            if (hostDescription.isLeader()) {
                addBehaviour(new HostsInformationLeaderListener(this));
                addBehaviour(new NotifyHostsInformationToMembers(this, 1000));
                addBehaviour(new NotifyHostsInformationToOtherLeaders(this, Consts.TICKS_FOR_FAILURE_NOTIFICATION_TO_LEADERS));

                addBehaviour(new NotifyFailuresToMembers(this, 1000));
                addBehaviour(new FailureLeaderListener(this));
                addBehaviour(new NotifyFailuresToOtherLeaders(this, Consts.TICKS_FOR_FAILURE_NOTIFICATION_TO_LEADERS));
                addBehaviour(new FailureSummariesListener(this));
            } else { // Only non-leader agent needs to receive information from leaders
                addBehaviour(new HostsInformationFromLeaderListener(this));
                addBehaviour(new HostsFailuresFromLeaderListener(this));
            }

            // Behaviours required for fault-tolerance.
            if (Consts.FAILURES_ARE_ENABLED) {
                // The SwitchFailureHandler behavior is disabled because so far no SwitchFailure are simulated
                //addBehaviour(new SwitchFailureHandler(this));

                addBehaviour(new UpdateStatusFailure(this, 1000));
                addBehaviour(new IncreaseLifeProgress(this, 1000));

                if (hostDescription.isLeader()) {
                    addBehaviour(new NotifyFailuresToMembers(this, 1000));
                    addBehaviour(new FailureLeaderListener(this));
                    addBehaviour(new NotifyFailuresToOtherLeaders(this, Consts.TICKS_FOR_FAILURE_NOTIFICATION_TO_LEADERS));
                    addBehaviour(new FailureSummariesListener(this));
                } else { // Only non-leader agent needs to receive information from leaders
                    addBehaviour(new HostsFailuresFromLeaderListener(this));
                }

            }

        } catch (Exception ex) {
            if (Consts.EXCEPTIONS) {
                System.out.println("It is here 8 " + ex);
            }
        }
    }


    private class PerformanceReporter extends TickerBehaviour {

        private ACLMessage msg;

        public PerformanceReporter(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            try {
                    msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(hostDescription.getMyLeader(), AID.ISLOCALNAME));
                    msg.setConversationId(Consts.CONVERSATION_MONITOR_HA);
                    msg.setContentObject((java.io.Serializable) hostDescription);
                    send(msg);
            } catch (Exception e) {
                if (Consts.EXCEPTIONS)
                    System.out.println("Hey 1178" + e);
            }
        }

    }

    private class MonitorHAListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;

        public MonitorHAListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_MONITOR_HA);
        }

        @Override
        public synchronized void action() {

            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {


                HostDescription aHostDescription = (HostDescription) msg.getContentObject();
                //System.out.println("I " + hostDescription.getId()+ " received " +  aHostDescription);
                Predicate<HostDescription> condition = hostDescription -> hostDescription.getId().equals(aHostDescription.getId());
                internalHostsInformation.removeIf(condition);
                internalHostsInformation.add(aHostDescription);
/*
                System.out.println("Leader, ..." + hostDescription.getId());
                for (HostDescription host : internalHostsInformation) {
                    System.out.println(host.getId()+" " + String.valueOf(host.getCPUUsage())+" " +String.valueOf(host.getMemoryUsage()));
                }
*/
            } catch (Exception ex) {

                if (Consts.EXCEPTIONS) {
                    System.out.println(ex);
                }
            }
        }
    }

    private String requestDecision() {

        DecisionRequest producer = new DecisionRequest();

        for (Map.Entry<String, Map<String, ArrayList<FailureRecord>>> entry : dataCenterFailures.entrySet()){
            //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            for (Map.Entry<String, ArrayList<FailureRecord>> subEntry : entry.getValue().entrySet()){
                //System.out.println("----------Key = " + subEntry.getKey() + ", Value = " + subEntry.getValue());
                ArrayList<FailureRecord> failures = subEntry.getValue();
                int i = 0;
                while (i < failures.size()) {
                    if (((FailureRecord)failures.get(i)).getEndFailure() <  System.currentTimeMillis() - Consts.MAXIMUM_FAILURE_HISTORY) {
                        failures.remove(i);
                    }
                    i++;
                }
            }
        }

        DataCenterInformation information = new DataCenterInformation(dataCenterHostsInformation, dataCenterFailures, Consts.TIME_WINDOW, System.currentTimeMillis());
        String json = new Gson().toJson(information);
        System.out.println(json);
        producer.produceMessages(json);

//        String json = new Gson().toJson(hostDescription);
        //System.out.println(json);


        // for testing purposes, right now it selects a coalition at random
        return coalitionLeaders.get((new Random()).nextInt(coalitionLeaders.size()));
    }

    private Map<String, ArrayList<String>> getCoalitions(ArrayList<HashSet<String>> setCoalitions) {
        Map<String, ArrayList<String>> tmpCoalitionToHosts = new HashMap<String, ArrayList<String>>();
        ArrayList<String> coalition = new ArrayList<String>();
        for (int coalitionNumber = 0; coalitionNumber < setCoalitions.size(); coalitionNumber++) {
            HashSet<String> set = setCoalitions.get(coalitionNumber);
            Iterator<String> iterator = set.iterator();
            ArrayList<String> hostAgents = new ArrayList<String>();
            ArrayList<Integer> hostAgentIDs = new ArrayList<Integer>();
            int minIdentifier = Integer.MAX_VALUE;
            while (iterator.hasNext()) {
                String anAgentID = (String) iterator.next();
                hostAgentIDs.add(Integer.valueOf(anAgentID));
                hostAgents.add("HostAgent" + anAgentID);
                if (Integer.valueOf(anAgentID) < minIdentifier) {
                    minIdentifier = Integer.valueOf(anAgentID);
                }
            }
            tmpCoalitionToHosts.put("HostAgent" + String.valueOf(minIdentifier), hostAgents);
        }
        return tmpCoalitionToHosts;
    }


    double getDistance(String source, String destination) {
        if (source.equals(destination)) {
            return 0;
        }


        Iterator<weightEdge> i = edges.iterator();
        while (i.hasNext()) {
            weightEdge edge = i.next();
            String in = "HostAgent" + edge.getInNode();
            String out = "HostAgent" + edge.getOutNode();
            if ((in.equals(source) && out.equals(destination)) || (in.equals(destination) && out.equals(source)))
                return edge.getDistance();
        }
        return -1;
    }

    private class IncreaseLifeProgress extends TickerBehaviour {

        public IncreaseLifeProgress(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            if (lifeProgress < Consts.LIFE_SPAN) {
                lifeProgress++;
            } else {
                lifeProgress = 0;
                // Frequency of data center server replacement worldwide 2018-2020
                // As per data from a recent report, in 2020, 42 percent of respondents
                // mentioned that they refreshed their data center servers every two to/
                // three years, whilst 26 percent stated that they did so every year.
                // https://www.statista.com/statistics/1109492/frequency-of-data-center-system-refresh-replacement-worldwide/
            }
        }

    }

    private class NotifyFailuresToOtherLeaders extends TickerBehaviour {

        private ACLMessage msg;

        public NotifyFailuresToOtherLeaders(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            try {
                // First, update the current leader's hosts failures
                dataCenterFailures.put(hostDescription.getId(), hostsFailures);

                // Then notify other leaders
                msg = new ACLMessage(ACLMessage.INFORM);
                for (int i = 0; i < coalitionLeaders.size(); i++) { // notify all the leaders (except myself) about the failures
                    if (!coalitionLeaders.get(i).equals("HostAgent" + hostDescription.getCoalition())) {
                        msg.addReceiver(new AID(coalitionLeaders.get(i), AID.ISLOCALNAME));
                    }
                }
                msg.setConversationId(Consts.CONVERSATION_FAILURE_SUMMARY_FROM_LEADERS);
                msg.setContentObject((java.io.Serializable) hostsFailures);
                send(msg);
            } catch (Exception e) {
                if (Consts.EXCEPTIONS) System.out.println("Hey 11343" + e);
            }
        }
    }


    private class NotifyHostsInformationToOtherLeaders extends TickerBehaviour {

        private ACLMessage msg;

        public NotifyHostsInformationToOtherLeaders(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            try {
                // First, update the current leader's hosts failures
                dataCenterHostsInformation.put(hostDescription.getId(), internalHostsInformation);

                // Then notify other leaders
                msg = new ACLMessage(ACLMessage.INFORM);
                for (int i = 0; i < coalitionLeaders.size(); i++) { // notify all the leaders (except myself) about the failures
                    if (!coalitionLeaders.get(i).equals("HostAgent" + hostDescription.getCoalition())) {
                        msg.addReceiver(new AID(coalitionLeaders.get(i), AID.ISLOCALNAME));
                    }
                }
                msg.setConversationId(Consts.CONVERSATION_HOSTS_INFORMATION);
                msg.setContentObject((java.io.Serializable) internalHostsInformation);
                send(msg);
            } catch (Exception e) {
                if (Consts.EXCEPTIONS) System.out.println("Hey 11343" + e);
            }
        }
    }


    private class FailureSummariesListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;

        public FailureSummariesListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_FAILURE_SUMMARY_FROM_LEADERS);
        }

        @Override
        public void action() {
            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                HashMap<String, ArrayList<FailureRecord>> aFailureSummary = (HashMap<String, ArrayList<FailureRecord>>) msg.getContentObject();
                //private Map<String, Map<String, ArrayList<FailureRecord>>> coalitionFailures; // coalition id, [host agent id, lists of Failures]
                dataCenterFailures.put(msg.getSender().getLocalName(), aFailureSummary);
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("Hey 1123484" + ex);
                }
            }

        }
    }


    private class UpdateStatusFailure extends TickerBehaviour {

        private ACLMessage msg;
        private Agent agt;
        private long failureStartTime;
        private long failureEndTime;

        public UpdateStatusFailure(Agent agt, long period) {
            super(agt, period);
            this.agt = agt;
        }

        @Override
        public synchronized void onTick() {
            //System.out.println(failureTicksDuration+ " "+ hostDescription.getCPUUsage() + " " +hostDescription.getId() + " " + failed );
            if (failureTicksDuration > 0) {
                failureTicksDuration--;
                if (failureTicksDuration <= 0) {
                    try {
                        //System.out.println(hostDescription.getId()+ " is up again");
                        failureEndTime = System.currentTimeMillis();

                        failed = false;
                        hostDescription.setFailed(false);
                        // Notify coalition leader that the server is up again
                        ACLMessage notifyBusinessAsUsualToLeader = new ACLMessage(ACLMessage.INFORM);
                        AID to = new AID(hostDescription.getId(), AID.ISLOCALNAME);
                        notifyBusinessAsUsualToLeader.setSender(agt.getAID());
                        notifyBusinessAsUsualToLeader.addReceiver(new AID(hostDescription.getMyLeader(), AID.ISLOCALNAME));
                        notifyBusinessAsUsualToLeader.setConversationId(Consts.CONVERSATION_FAILURE_NOTIFICATION);
                        FailureRecord failure = new FailureRecord(failureStartTime, failureEndTime);
                        notifyBusinessAsUsualToLeader.setContentObject(failure);
                        System.out.println("{" + "\"hostAgentId\":\"" + hostDescription.getId() + "\", " + "\"coalitionId\":" + hostDescription.getCoalition() + ", " + "\"failureDuration\":" + failure.getFailureDuration() + "}");
                        agt.send(notifyBusinessAsUsualToLeader);
                    } catch (IOException ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("Hey Failure 1" + ex);
                        }
                    }
                }
            }
            if ((!failed) && (failureGeneration.failed(lifeProgress)) && !hostDescription.isInProgress()) {
                resetAverageUsages();
                resetCounters();
                resetThresholdFlags();
                failed = true;
                hostDescription.setFailed(true);
                // Normally distributed failure duration
                // in the range [1, Consts.MEAN_FAILURE_DURATION + Consts.STD_DEV_FAILURE_DURATION * 2]
                Random randomGenerator = new Random();
                failureTicksDuration = Math.round(Consts.MEAN_FAILURE_DURATION + randomGenerator.nextGaussian() * Consts.STD_DEV_FAILURE_DURATION);
                if (failureTicksDuration <= 0)
                    failureTicksDuration = 1;
                else if (failureTicksDuration > (Consts.MEAN_FAILURE_DURATION + Consts.STD_DEV_FAILURE_DURATION * 2))
                    failureTicksDuration = (long) (Consts.MEAN_FAILURE_DURATION + Consts.STD_DEV_FAILURE_DURATION * 2);
                failureStartTime = System.currentTimeMillis();
                failureEndTime = -1; // meaning it has not been defined yet
            }

        }
    }

    private class LeaderListenerForCounterReset extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage initialMsg;
        private ACLMessage finalMsg;
        private AID to;

        public LeaderListenerForCounterReset(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_A_COALITION_WAS_JUST_BALANCED);
        }

        @Override
        public void action() {
            initialMsg = receive(mt);
            if (initialMsg == null) {
                block();
                return;
            }
            try {
                resetAverageUsages();
                resetCounters();
                //System.out.println(hostDescription.getId()+"- I have to notify my members");
                // send a message to all coalitions members

                finalMsg = new ACLMessage(ACLMessage.REQUEST);
                to = new AID(hostDescription.getId(), AID.ISLOCALNAME);
                finalMsg.setSender(agt.getAID());

                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(hostDescription.getMyLeader());
                for (int i = 0; i < coalitionMembers.size(); i++) { // notify all the coalition members (except me) that a coalition has been balanced
                    if (!coalitionMembers.get(i).equals(hostDescription.getId())) {
                        finalMsg.addReceiver(new AID(coalitionMembers.get(i), AID.ISLOCALNAME));
                    }
                }
                finalMsg.setConversationId(Consts.CONVERSATION_RESET_COUNTERS);
                finalMsg.setContent("nothing relevant");
                //System.out.println(hostDescription.getId()+"- membersNotified");
                agt.send(finalMsg);

            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("Hey 1" + ex);
                }
            }
        }
    }

    private class MemberListenerForCounterReset extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;

        public MemberListenerForCounterReset(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_RESET_COUNTERS);
        }

        @Override
        public void action() {
            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                resetAverageUsages();
                resetCounters();
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("Hey 2 " + ex);
                }
            }
        }
    }

    private class ResetDatacenterLoadBalancingCounters extends OneShotBehaviour {

        private Agent agt;

        public ResetDatacenterLoadBalancingCounters(Agent agt) {
            super(null);
            this.agt = agt;
        }

        @Override
        public void action() {
            try {

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                AID to = new AID(hostDescription.getId(), AID.ISLOCALNAME);
                msg.setSender(agt.getAID());
                for (int i = 0; i < coalitionLeaders.size(); i++) { // notify all the leaders (except my own leader) that a coalition has been balanced
                    if (!coalitionLeaders.get(i).equals("HostAgent" + hostDescription.getCoalition())) {
                        msg.addReceiver(new AID(coalitionLeaders.get(i), AID.ISLOCALNAME));
                    }
                }
                msg.setConversationId(Consts.CONVERSATION_A_COALITION_WAS_JUST_BALANCED);
                msg.setContent("nothing relevant");
                //System.out.println(hostDescription.getId()+"- notifying leaders that I have migrated a VM");
                agt.send(msg);
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("Hey 4" + ex);
                }
            }


        }
    }

    private class VMWARE_ListenerForVMMigrations extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;

        public VMWARE_ListenerForVMMigrations(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.VMWARE_CONVERSATION_CONFIRM_MIGRATION);
        }

        @Override
        public void action() {

            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                vm = (VirtualMachineDescription) (msg.getContentObject());
                operationOverVM(vm, "removeAndMigrate", "AtoB", null, null);
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 9" + ex);
                }
            }
            hostDescription.setInProgress(false);
        }
    }

    private class VMWARE_LockResources extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;
        private ACLMessage acknowledgementMsg;

        public VMWARE_LockResources(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.VMWARE_CONVERSATION_LOCK_RESOURCES);
        }

        @Override
        public void action() {

            msg = receive(mt);
            if (msg == null) {
                hostDescription.setInProgress(false);
                block();
                return;
            }
            try {
                if (!hostDescription.isInProgress()) {

                    hostDescription.setInProgress(true);
                    vm = (VirtualMachineDescription) (msg.getContentObject());
                    if ((vm.getMemory() <= hostDescription.getAvailableMemory()) && (vm.getNumberOfVirtualCores() <= hostDescription.getAvailableVirtualCores())) { // if the host has sufficient resources to allocate the VM
                        acknowledgementMsg = new ACLMessage(ACLMessage.CONFIRM);
                        acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_RESOURCES);
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Success in locking resources for VM migration");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Success in locking resources for VM migration");
                        }
                        //System.out.println("HERE He is 4");
                    } else {
                        hostDescription.setInProgress(false);
                        acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                        acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_RESOURCES);
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Failed to lock resources for VM migration due to insufficient resources");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Failed to lock resources for VM migration due to insufficient resources");
                        }
                        //System.out.println("HERE He is 5");
                    }
                } else { //if it is busy
                    acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                    acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_RESOURCES);
                    acknowledgementMsg.addReceiver(msg.getSender());
                    acknowledgementMsg.setContent("Failed to lock resources for VM migration because I'm busy");
                    agt.send(acknowledgementMsg);
                }
            } catch (Exception ex) {
                hostDescription.setInProgress(false);
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 10" + ex);
                }

            }

        }
    }

    private class VMWARE_LockVM extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;
        private ACLMessage acknowledgementMsg;

        public VMWARE_LockVM(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.VMWARE_CONVERSATION_LOCK_VM);
        }

        @Override
        public void action() {

            msg = receive(mt);
            if (msg == null) {
                hostDescription.setInProgress(false);
                block();
                return;
            }
            try {
                if (!hostDescription.isInProgress()) {
                    hostDescription.setInProgress(true);

                    vm = (VirtualMachineDescription) (msg.getContentObject());

                    if (hostDescription.isVirtualMachineHosted(vm.getId())) {
                        acknowledgementMsg = new ACLMessage(ACLMessage.CONFIRM);
                        acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_VM);
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Success in locking the VM");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Success in locking the VM");
                        }
                    } else {
                        hostDescription.setInProgress(false);
                        acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                        acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_VM);
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Failed to lock the VM. The VM is not here");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Failed to lock the VM. The VM is not here");
                        }
                    }

                } else { // if in progress, cancel protocol
                    acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                    acknowledgementMsg.setConversationId(Consts.VMWARE_CONVERSATION_LOCK_VM);
                    acknowledgementMsg.addReceiver(msg.getSender());
                    acknowledgementMsg.setContent("I'm busy");
                    agt.send(acknowledgementMsg);
                }
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 11" + ex);
                }
                hostDescription.setInProgress(false);
            }
        }


    }


    private class VMWARE_Unlock extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;

        public VMWARE_Unlock(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.VMWARE_CONVERSATION_UNLOCK);
        }

        @Override
        public void action() {
            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }

            hostDescription.setInProgress(false);
        }
    }


    private void resetAverageUsages() {
        currentTick = -1;
        for (int i = 0; i < lastCPUUsages.length; i++) {
            lastCPUUsages[i] = 0;
            lastMemoryUsages[i] = 0;
        }
    }

    private void resetThresholdFlags() {
        highCPUThresholdViolated = false;
        lowCPUThresholdViolated = false;
        highMemoryThresholdViolated = false;
        lowMemoryThresholdViolated = false;
    }

    private void resetCounters() {
        thresholdViolationCounterForHighMemory = 0;
        thresholdViolationCounterForHighCPU = 0;
        thresholdViolationCounterForLowMemory = 0;
        thresholdViolationCounterForLowCPU = 0;
    }

    private class ListenerForVMMigrations extends CyclicBehaviour { // This behaviour is involved in both VMWARE load balancing and Distributed Load Balancing

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;

        public ListenerForVMMigrations(Agent a) {
            this.agt = a;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_REGISTRATION_DUE_TO_VM_MIGRATION);
        }

        @Override
        public void action() {
            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            } else {

                try {

                    vm = (VirtualMachineDescription) (msg.getContentObject());
                    if (operationOverVM(vm, "migration", null, null, null) && !failed) { // if it can host the VM
                        if (Consts.LOG) {
                            //System.out.println(hostDescription.getId() + " successful migration of VM: " + vm);

                            int sourceCoalition = Integer.valueOf(hostAgentToCoalition.get(vm.getPreviousOwnerId()).replace("HostAgent", ""));
                            int destinationCoalition = Integer.valueOf(hostAgentToCoalition.get(vm.getOwnerId()).replace("HostAgent", ""));
                            System.out.println("{\"source_coalition\":" + String.valueOf(sourceCoalition) + ", \"destination_coalition\":" + String.valueOf(destinationCoalition) + ", \"migrationType\":\"" + vm.getMigrationType() + "\"" + ", \"origin\":\"" + vm.getPreviousOwnerId() + "\"" + ", \"destination\":\"" + vm.getOwnerId() + "\"" + ", \"vmid\":\"" + vm.getId() + "\"" + ", \"distance\":" + getDistance(vm.getPreviousOwnerId(), vm.getOwnerId()) + ", \"time\":" + System.currentTimeMillis() + "}");
                            //}
                        }
                    } else { // it cannot host the vm
                        if (!Consts.LOG) {
                            System.out.println(hostDescription.getId() + " failed to migrate VM: " + vm);
                        }
                    }

                } catch (Exception ex) {
                    if (Consts.EXCEPTIONS) {
                        System.out.println("It is here 14" + ex);
                    }
                }
                resetAverageUsages();
                resetCounters();
                resetThresholdFlags();
                hostDescription.setInProgress(false);
            }


        }
    }

    private class RequestsReceiver extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;
        private ACLMessage acknowledgementMsg;
        private Object[] vmAgentParams;

        public RequestsReceiver(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_VM_ALLOCATION);
        }

        @Override
        public void action() {
            if (!hostDescription.isInProgress()) {

                msg = receive(mt);
                if (msg == null) {
                    hostDescription.setInProgress(false);
                    block();
                    return;
                }
                try {
                    hostDescription.setInProgress(true);
                    vm = (VirtualMachineDescription) (msg.getContentObject());
                    if (operationOverVM(vm, "initialAllocation", null, null, null) && !failed) { // if it can host the VM
                        acknowledgementMsg = new ACLMessage(ACLMessage.INFORM);
                        acknowledgementMsg.setConversationId(vm.getConversationId());
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Successful allocation");
                        if (!Consts.LOG) {
                            System.out.println("Successful allocation " + vm.getVirtualMachineId());
                        }
                        agt.send(acknowledgementMsg);
                        //Create VM agent;
                        vmAgentParams = new Object[1];
                        vm.setOwnerId(hostDescription.getId());
                        vmAgentParams[0] = vm;
                        getContainerController().createNewAgent(vm.getVirtualMachineId(), "intraloadbalancingft.VirtualMachineAgent", vmAgentParams);
                        getContainerController().getAgent(String.valueOf(vm.getVirtualMachineId())).start();
                    } else { // it cannot host the vm

                        // To implement fault-tolerance, we may need to customize this message indicating the reason behind the failure

                        acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                        acknowledgementMsg.setConversationId(vm.getConversationId());
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Failed allocation. The server cannot host the VM");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Failed allocation. The server cannot host the VM");
                        }
                    }

                } catch (Exception ex) {
                    hostDescription.setInProgress(false);
                    if (Consts.EXCEPTIONS) {
                        System.out.println("It is here 16" + ex);
                    }
                }
                hostDescription.setInProgress(false);
            }

        }
    }

    private class HostsFailuresFromLeaderListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;

        public HostsFailuresFromLeaderListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_HOSTS_FAILURES_FROM_LEADER);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                dataCenterFailures  = (Map<String, Map<String, ArrayList<FailureRecord>>> ) msg.getContentObject();
                //System.out.println("------"+hostDescription.getId()+"------\n"+dataCenterFailures);
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 116" + ex);
                }
            }
        }
    }

    private class HostsInformationLeaderListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;

        public HostsInformationLeaderListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_HOSTS_INFORMATION);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {

                ArrayList<HostDescription> aListOfHostDescriptions = (ArrayList<HostDescription>) msg.getContentObject();
                dataCenterHostsInformation.put(msg.getSender().getLocalName(), aListOfHostDescriptions);

                // getting information of the hostAgent's coalition members
                ArrayList<HostDescription> coalitionMembers = dataCenterHostsInformation.get(hostDescription.getMyLeader());

                int[] thresholds = {0, configuration.getTARGET_STD_DEV(), 0, configuration.getTARGET_STD_DEV()};
                if (coalitionMembers != null) {
                    if (coalitionMembers.size() > 0) {
                        thresholds = calculateNewThresholds(coalitionMembers);
                    }
                }
                hostDescription.setLowMigrationThresholdForCPU(thresholds[0]);
                hostDescription.setHighMigrationThresholdForCPU(thresholds[1]);
                hostDescription.setLowMigrationThresholdForMemory(thresholds[2]);
                hostDescription.setHighMigrationThresholdForMemory(thresholds[3]);


                // iterating through key/value mappings
                /*
                System.out.println("Entries: ");
                for(Map.Entry<String, ArrayList<HostDescription>> entry: dataCenterHostsInformation.entrySet()) {
                    System.out.println(entry.getKey());
                    for (HostDescription host : entry.getValue()) {
                        System.out.println(host.getId()+" " + String.valueOf(host.getCPUUsage())+" " +String.valueOf(host.getMemoryUsage()));
                    }
                }
                */



            } catch (Exception ex) {

                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 889" + ex);
                }
            }
        }
    }

    private class HostsInformationFromLeaderListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;

        public HostsInformationFromLeaderListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_HOSTS_INFORMATION_FROM_LEADER);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {

                dataCenterHostsInformation = (Map<String, ArrayList<HostDescription>>) msg.getContentObject();

                ArrayList<HostDescription> coalitionMembers = dataCenterHostsInformation.get(hostDescription.getMyLeader());

                int[] thresholds = {0, configuration.getTARGET_STD_DEV(), 0, configuration.getTARGET_STD_DEV()};
                if (coalitionMembers != null) {
                    if (coalitionMembers.size() > 0) {
                        thresholds = calculateNewThresholds(coalitionMembers);
                    }
                }
                hostDescription.setLowMigrationThresholdForCPU(thresholds[0]);
                hostDescription.setHighMigrationThresholdForCPU(thresholds[1]);
                hostDescription.setLowMigrationThresholdForMemory(thresholds[2]);
                hostDescription.setHighMigrationThresholdForMemory(thresholds[3]);

                /*
                System.out.println("******************" + hostDescription.getId()+"'s Entries: ");
                for(Map.Entry<String, ArrayList<HostDescription>> entry: dataCenterHostsInformation.entrySet()) {
                    System.out.println("******************" + hostDescription.getId()+" ------ " + entry.getKey());
                    for (HostDescription host : entry.getValue()) {
                        System.out.println("******************" + hostDescription.getId()+" ------ " + host.getId()+" " + String.valueOf(host.getCPUUsage())+" " +String.valueOf(host.getMemoryUsage())+ " "+ host.isFailed()) ;
                    }

                } */

            } catch (Exception ex) {

                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 890" + ex);
                }
            }
        }
    }



    private class FailureLeaderListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;

        public FailureLeaderListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_FAILURE_NOTIFICATION);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {

                FailureRecord failureRecord = (FailureRecord) msg.getContentObject();
                ArrayList<FailureRecord> failures = hostsFailures.get(msg.getSender().getLocalName());
                if (failures == null) failures = new ArrayList<FailureRecord>();

                failures.add(failureRecord);
                hostsFailures.put(msg.getSender().getLocalName(), failures);

            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 888" + ex);
                }
            }
        }
    }


    private class SwitchFailureHandler extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private ACLMessage acknowledgementMsg;
        private Object[] vmAgentParams;

        public SwitchFailureHandler(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.FAILURE_FROM_SWITCH);
        }

        @Override
        public void action() {

            // TWO TYPES OF FAILURES HOST AND SWITCH
            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                //System.out.println(msg.getContent());
                if (msg.getContent().equals("FAILED")) {
                    failed = true;
                    hostDescription.setFailed(true);
                } else { // if switch working again
                    failed = false;
                    hostDescription.setFailed(false);
                }
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here Failure 1" + ex);
                }
            }
        }
    }

    public static Object createModel() {
        return null;
    }



    private class NotifyFailuresToMembers extends TickerBehaviour {

        private ACLMessage msg;

        public NotifyFailuresToMembers(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {

            try {
                msg = new ACLMessage(ACLMessage.INFORM);
                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(hostDescription.getMyLeader());
                for (int i = 0; i < coalitionMembers.size(); i++) {
                    if (!coalitionMembers.get(i).equals(hostDescription.getId())) {
                        msg.addReceiver(new AID(coalitionMembers.get(i), AID.ISLOCALNAME));
                    }
                }
                msg.setConversationId(Consts.CONVERSATION_HOSTS_FAILURES_FROM_LEADER);
                //msg.setContentObject((java.io.Serializable) logisticRegressionModel);
                msg.setContentObject((java.io.Serializable) dataCenterFailures);

                String json = new Gson().toJson(dataCenterFailures);
                //System.out.println("-------"+json);
                send(msg);
            } catch (Exception e) {
                if (Consts.EXCEPTIONS) System.out.println("Hey 1143242" + e);
            }
        }

    }

    private class NotifyHostsInformationToMembers extends TickerBehaviour {

        private ACLMessage msg;

        public NotifyHostsInformationToMembers(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {

            try {
                msg = new ACLMessage(ACLMessage.INFORM);
                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(hostDescription.getMyLeader());
                for (int i = 0; i < coalitionMembers.size(); i++) {
                    if (!coalitionMembers.get(i).equals(hostDescription.getId())) {
                        msg.addReceiver(new AID(coalitionMembers.get(i), AID.ISLOCALNAME));
                    }
                }
                msg.setConversationId(Consts.CONVERSATION_HOSTS_INFORMATION_FROM_LEADER);
                msg.setContentObject((java.io.Serializable) dataCenterHostsInformation);

                String json = new Gson().toJson(dataCenterHostsInformation);
                //System.out.println("-------"+json);
                send(msg);
            } catch (Exception e) {
                System.out.println("Hey dsadas223" + e);
                if (Consts.EXCEPTIONS) System.out.println("Hey 1143242" + e);
            }
        }

    }


    private class VMWARE_RemoveAndMigrateVM extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;
        private ACLMessage acknowledgementMsg;
        private Object[] vmAgentParams;

        public VMWARE_RemoveAndMigrateVM(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.VMWARE_CONVERSATION_VM_MIGRATION);
        }

        @Override
        public void action() {
            if (!hostDescription.isInProgress()) {
                msg = receive(mt);
                if (msg == null) {
                    block();
                    hostDescription.setInProgress(false);
                    return;
                }
                try {
                    hostDescription.setInProgress(true);
                    vm = (VirtualMachineDescription) (msg.getContentObject());
                    if (operationOverVM(vm, "initialAllocation", null, null, null)) { // if it can host the VM
                        acknowledgementMsg = new ACLMessage(ACLMessage.INFORM);
                        acknowledgementMsg.setConversationId(vm.getConversationId());
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Successful allocation");
                        if (!Consts.LOG) {
                            System.out.println("Successful allocation " + vm.getVirtualMachineId());
                        }
                        agt.send(acknowledgementMsg);
                        //Create VM agent;
                        vmAgentParams = new Object[2];
                        vm.setOwnerId(hostDescription.getId());

                        vmAgentParams[0] = vm;
                        getContainerController().createNewAgent(vm.getVirtualMachineId(), "intraloadbalancingft.VirtualMachineAgent", vmAgentParams);
                        getContainerController().getAgent(String.valueOf(vm.getVirtualMachineId())).start();
                    } else { // it cannot host the vm
                        acknowledgementMsg = new ACLMessage(ACLMessage.FAILURE);
                        acknowledgementMsg.setConversationId(vm.getConversationId());
                        acknowledgementMsg.addReceiver(msg.getSender());
                        acknowledgementMsg.setContent("Failed allocation. The server cannot host the VM");
                        agt.send(acknowledgementMsg);
                        if (!Consts.LOG) {
                            System.out.println("Failed allocation. The server cannot host the VM");
                        }
                    }
                } catch (Exception ex) {
                    if (Consts.EXCEPTIONS) {
                        System.out.println("It is here 17" + ex);
                    }
                }
                hostDescription.setInProgress(false);
            }

        }
    }

    private class MonitorVMListener extends CyclicBehaviour {

        private Agent agt;
        private MessageTemplate mt;
        private ACLMessage msg;
        private VirtualMachineDescription vm;

        public MonitorVMListener(Agent agt) {
            this.agt = agt;
            this.mt = MessageTemplate.MatchConversationId(Consts.CONVERSATION_MONITOR_VM);
        }

        @Override
        public synchronized void action() {

            msg = receive(mt);
            if (msg == null) {
                block();
                return;
            }
            try {
                vm = (VirtualMachineDescription) (msg.getContentObject());
                updateVirtualMachineResourceConsumption(vm);
                updateHostResourceConsumption();
            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 18" + ex);
                }
            }

        }
    }

    private void updateVirtualMachineResourceConsumption(VirtualMachineDescription vmDescriptionToBeUpdated) {
        try {
            for (int i = 0; i < hostDescription.getVirtualMachinesHosted().size(); i++) {
                if (hostDescription.getVirtualMachinesHosted().get(i).getId().equals(vmDescriptionToBeUpdated.getId())) {
                    hostDescription.getVirtualMachinesHosted().get(i).setCPUUsage(vmDescriptionToBeUpdated.getCPUUsage());
                    hostDescription.getVirtualMachinesHosted().get(i).setMemoryUsage(vmDescriptionToBeUpdated.getMemoryUsage());
                    break;
                }
            }

        } catch (Exception ex) {
            if (Consts.EXCEPTIONS) {
                System.out.println("It is here 19" + ex);
            }
        }
    }

    private void updateHostResourceConsumption() {
        double sumMemoryUsage = 0;  // percentage
        double sumCPUUsage = 0;  // percentage
        double memoryUsage = 0;
        double CPUUsage = 0;
        for (int i = 0; i < hostDescription.getVirtualMachinesHosted().size(); i++) {
            sumCPUUsage = sumCPUUsage + ((hostDescription.getVirtualMachinesHosted().get(i).getCPUUsage() / 100) * hostDescription.getVirtualMachinesHosted().get(i).getNumberOfVirtualCores());
            sumMemoryUsage = sumMemoryUsage + (hostDescription.getVirtualMachinesHosted().get(i).getMemoryUsage() / 100) * hostDescription.getVirtualMachinesHosted().get(i).getMemory();
        }
        if (hostDescription.getVirtualMachinesHosted().size() > 0) {
            memoryUsage = (100 * sumMemoryUsage) / hostDescription.getMemory();
            if (memoryUsage > 100) {
                memoryUsage = 100;
            }
            if (!failed) hostDescription.setMemoryUsage(memoryUsage);
            else hostDescription.setMemoryUsage(0);
            CPUUsage = (100 * sumCPUUsage) / hostDescription.getNumberOfVirtualCores();
            if (CPUUsage > 100) {
                CPUUsage = 100;
            }
            if (!failed) hostDescription.setCPUUsage(CPUUsage);
            else hostDescription.setCPUUsage(0);
        } else {
            hostDescription.setMemoryUsage(0);
            hostDescription.setCPUUsage(0);
        }


        hostAgentGUI.updateResourceConsumption();
    }

    public class VirtualMachineKiller extends SimpleBehaviour {

        private long timeout;
        private long wakeupTime;
        private boolean terminated = false;
        private Agent agt;

        public VirtualMachineKiller(Agent agt, long timeout) {
            this.agt = agt;
            this.timeout = timeout;
        }

        @Override
        public void onStart() {
            wakeupTime = System.currentTimeMillis() + timeout;
        }

        @Override
        public void action() {
            long dt = wakeupTime - System.currentTimeMillis();
            if (dt <= 0) {
                handleElapsedTimeout();
            } else {
                block(dt);
            }
        }

        protected void handleElapsedTimeout() {
            if (!hostDescription.isInProgress() && !failed) {
                hostDescription.setInProgress(true);
                resetAverageUsages();
                resetCounters();
                resetThresholdFlags();
                terminated = true;
                if (hostDescription.getVirtualMachinesHosted().size() > 0) {
                    operationOverVM(null, "randomDeparture", null, null, null);
                }
                hostDescription.setInProgress(false);
            }
            //System.out.println(hostDescription.getId()+" attempting to kill");
        }

        @Override
        public boolean done() {
            if (terminated) {
                long delay = (long) (Consts.AVG_INTERDEPARTURE_TIME * (-Math.log(Math.random()))); //  Departure process is Poisson Distributed
                agt.addBehaviour(new VirtualMachineKiller(agt, delay));
            }
            return terminated;
        }
    }

    private VirtualMachineDescription randomlySelectVMForMigration() {
        ArrayList<VirtualMachineDescription> availableVMs = new ArrayList<>(hostDescription.getVirtualMachinesHosted());
        Predicate<VirtualMachineDescription> condition = virtualMachineDescription -> virtualMachineDescription.isLock() == true;
        availableVMs.removeIf(condition);

        if (availableVMs.size() > 0) {
            return availableVMs.get((new Random()).nextInt(availableVMs.size()));// If a VM can be migrated
        } else {
            return null;
        }

    }

    private boolean operationOverVM(VirtualMachineDescription vm, String operation, String type, String source, String destination) { // This methods is only executed when inProgress is set to False. This prevents datarace conditions due to behaviours' concurrent access to VMs
        switch (operation) {

            case "initialAllocation":
                if (vm.getMemory() <= hostDescription.getAvailableMemory() && vm.getNumberOfVirtualCores() <= hostDescription.getAvailableVirtualCores()) { // if the host has sufficient resources to allocate the VM
                    hostDescription.setMemoryUsed(hostDescription.getMemoryUsed() + vm.getMemory());
                    hostDescription.setNumberOfVirtualCoresUsed(hostDescription.getNumberOfVirtualCoresUsed() + vm.getNumberOfVirtualCores());
                    try {
                        vm.setContainerName(this.getContainerController().getContainerName());
                    } catch (Exception ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 20" + ex);
                        }
                    }
                    vm.setPreviousOwnerId(hostDescription.getId());
                    vm.setOwnerId(hostDescription.getId());

                    hostDescription.getVirtualMachinesHosted().add(vm);
                    updateHostResourceConsumption();
                    return true; // success
                }
                return false; // failed

            case "migration":
                if (vm.getMemory() <= hostDescription.getAvailableMemory() && vm.getNumberOfVirtualCores() <= hostDescription.getAvailableVirtualCores()) { // if the host has sufficient resources to allocate the VM
                    hostDescription.setMemoryUsed(hostDescription.getMemoryUsed() + vm.getMemory());
                    hostDescription.setNumberOfVirtualCoresUsed(hostDescription.getNumberOfVirtualCoresUsed() + vm.getNumberOfVirtualCores());
                    try {
                        vm.setContainerName(this.getContainerController().getContainerName());
                    } catch (Exception e) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 21" + e);
                        }
                    }
                    hostDescription.getVirtualMachinesHosted().add(vm);
                    updateHostResourceConsumption();
                    return true; // success
                } else {
                    if (!Consts.LOG) {
                        System.out.println("ERROR to allocate VM: insufficient resources");
                    }
                }
                return false; // failed

            case "randomDeparture":
                if (hostDescription.getVirtualMachinesHosted().size() > 0) { // If a VM can be removed
                    VirtualMachineDescription randomlySelectedVM = hostDescription.getVirtualMachinesHosted().get((new Random()).nextInt(hostDescription.getVirtualMachinesHosted().size()));
                    hostDescription.getVirtualMachinesHosted().remove(randomlySelectedVM);
                    try {
                        //System.out.println(hostDescription.getId()+" "+randomlySelectedVM.getVirtualMachineId());
                        getContainerController().getAgent(randomlySelectedVM.getVirtualMachineId()).suspend();
                        getContainerController().getAgent(randomlySelectedVM.getVirtualMachineId()).kill();
                        if (!Consts.LOG) {
                            System.out.println(hostDescription.getId() + " killed " + randomlySelectedVM.getVirtualMachineId());
                        }
                        hostDescription.setMemoryUsed(hostDescription.getMemoryUsed() - randomlySelectedVM.getMemory());
                        hostDescription.setNumberOfVirtualCoresUsed(hostDescription.getNumberOfVirtualCoresUsed() - randomlySelectedVM.getNumberOfVirtualCores());
                        updateHostResourceConsumption();
                        return true; // success
                    } catch (jade.wrapper.ControllerException e) {
                        // VM already removed
                    } catch (Exception e) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 22" + e);
                        }
                    }
                }
                return false; // failed 
            case "removeAndMigrate":
                if (!Consts.LOG) {
                    System.out.println("VM ready to be deleted and migrated " + vm.getId());
                }
//                System.out.println("VM ready to be deleted and migrated " + vm.getId()+ " from "+ vm.getPreviousOwnerId() + " to "+ vm.getOwnerId()+ " who printed "+ hostDescription.getId());
                Predicate<VirtualMachineDescription> conditionForRemoval = virtualMachineDescription -> virtualMachineDescription.getId().equals(vm.getId());
                if (hostDescription.getVirtualMachinesHosted().removeIf(conditionForRemoval)) { // If the VM was found and can be removed.
                    try {
                        hostDescription.setMemoryUsed(hostDescription.getMemoryUsed() - vm.getMemory());
                        hostDescription.setNumberOfVirtualCoresUsed(hostDescription.getNumberOfVirtualCoresUsed() - vm.getNumberOfVirtualCores());
                        updateHostResourceConsumption();

                        ACLMessage migrationRequest = new ACLMessage(ACLMessage.REQUEST);
                        migrationRequest.setConversationId(Consts.CONVERSATION_MIGRATE);
                        migrationRequest.addReceiver(new AID(vm.getVirtualMachineId(), AID.ISLOCALNAME));
                        vm.setMigrationType(type);
                        migrationRequest.setContentObject((VirtualMachineDescription) vm);
                        send(migrationRequest);

                        if (!Consts.LOG) {
                            System.out.println(hostDescription.getId() + " at " + this.getContainerController().getContainerName() + " migrated " + vm.getVirtualMachineId() + " to " + vm.getOwnerId() + " at " + vm.getContainerName());
                        }
                        resetAverageUsages();
                        resetCounters();
                        resetThresholdFlags();
                        hostDescription.setInProgress(false);
                        return true; // success
                    } catch (Exception e) {
                        hostDescription.setInProgress(false);
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 23" + e);
                        }
                    }
                } else {
                    if (!Consts.LOG) {
                        System.out.println("Error: failure to remove VM prior to migrate it to other host");
                    }
                    resetAverageUsages();
                    resetCounters();
                    resetThresholdFlags();
                    hostDescription.setInProgress(false);
                }
                return false; // failed

            default:
                if (!Consts.LOG) {
                    System.out.println("ERROR: unknown operation type over VM.");
                }
        }

        return false; // failure                               

    }


    private class PerformanceReporterAndThresholdMonitoring extends TickerBehaviour {

        private Agent agt;
        private ACLMessage msg;
        private double totalCPUUsage = 0;
        private double totalMemoryUsage = 0;

        public PerformanceReporterAndThresholdMonitoring(Agent agt, long period) {
            super(agt, period);
            this.agt = agt;
            currentTick = -1;
        }

        @Override
        protected void onTick() {

            try {
                updateHostResourceConsumption();
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(hostDescription.getAllocatorId(), AID.ISLOCALNAME));
                msg.setConversationId(Consts.CONVERSATION_MONITOR_HOST);
                msg.setContentObject((java.io.Serializable) hostDescription);
                send(msg);
                if ((configuration.getLOAD_BALANCING_TYPE() == Consts.DISTRIBUTED_FIXED_COALITIONS) && (!failed)) {

                    if (Consts.MIGRATION_TRIGGER_TYPE == Consts.MIGRATION_TRIGGER_BASED_ON_COUNTERS) {

                        if (hostDescription.getCPUUsage() > hostDescription.getHighMigrationThresholdForCPU()) {
                            thresholdViolationCounterForHighCPU++;
                        } else if (thresholdViolationCounterForHighCPU > 0) {
                            thresholdViolationCounterForHighCPU--;
                        }

                        if (hostDescription.getMemoryUsage() > hostDescription.getHighMigrationThresholdForMemory()) {
                            thresholdViolationCounterForHighMemory++;
                        } else if (thresholdViolationCounterForHighMemory > 0) {
                            thresholdViolationCounterForHighMemory--;
                        }

                        if (hostDescription.getCPUUsage() < hostDescription.getLowMigrationThresholdForCPU()) {
                            thresholdViolationCounterForLowCPU++;
                        } else if (thresholdViolationCounterForLowCPU > 0) {
                            thresholdViolationCounterForLowCPU--;
                        }

                        if (hostDescription.getMemoryUsage() < hostDescription.getLowMigrationThresholdForMemory()) {
                            thresholdViolationCounterForLowMemory++;
                        } else if (thresholdViolationCounterForLowMemory > 0) {
                            thresholdViolationCounterForLowMemory--;
                        }
                        // verifying whether any counter cause a vm migration from this host agent or other host agent from the same coalition
                        if ((thresholdViolationCounterForHighCPU >= Consts.MAX_THRESHOLD_VIOLATION_COUNTER_FOR_HIGH_CPU) && Consts.BALANCE_CPU && (!hostDescription.isInProgress())) {
                            hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                            resetCounters();
                            resetAverageUsages();
                            highCPUThresholdViolated = true;
                            /*
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * */
                            String coalitionIdForLoadBalancing = "";
                            coalitionIdForLoadBalancing = requestDecision();

                            //System.out.println("I should start a VM migration from A to B");
                            if ((coalitionToHostAgents != null) && (coalitionLeaders != null) && (dataCenterHostsInformation != null)) {
                                /////////// Randomly selecting a destination HostAgent and a VM.
                                //System.out.println("A0");
                                //String coalitionId = coalitionLeaders.get(new Random().nextInt(coalitionToHostAgents.size()));
                                String coalitionId = coalitionIdForLoadBalancing;
                                //System.out.println("A1");
                                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionId);
                                //System.out.println("A2");
                                HostDescription destinationHost = dataCenterHostsInformation.get(coalitionId).get(new Random().nextInt(coalitionMembers.size()));
                                //System.out.println("A3");
                                //System.out.println("DestinationHost " + destinationHost);

                                VirtualMachineDescription selectedVM = new VirtualMachineDescription();
                                if (hostDescription.getVirtualMachinesHosted().size() > 0) {
                                    selectedVM = hostDescription.getVirtualMachinesHosted().get(new Random().nextInt(hostDescription.getVirtualMachinesHosted().size()));
                                }

                                Decision decision = new Decision(hostDescription, destinationHost, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_A_TO_B);
                                ///////////////////////////////////////////////////////////////

                                agt.addBehaviour(new InitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_CPU, decision));
                            }
                        } else if ((thresholdViolationCounterForHighMemory >= Consts.MAX_THRESHOLD_VIOLATION_COUNTER_FOR_HIGH_MEMORY) && Consts.BALANCE_MEMORY && (!hostDescription.isInProgress())) {
                            hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                            resetCounters();
                            resetAverageUsages();
                            highMemoryThresholdViolated = true;
                            /*
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * */
                            String coalitionIdForLoadBalancing = "";
                            coalitionIdForLoadBalancing = requestDecision();

                            //System.out.println("I should start a VM migration from A to B");
                            if ((coalitionToHostAgents != null) && (coalitionLeaders != null) && (dataCenterHostsInformation != null)) {
                                /////////// Randomly selecting a destination HostAgent and a VM.
                                //System.out.println("B0");
                                // String coalitionId = coalitionLeaders.get(new Random().nextInt(coalitionToHostAgents.size()));
                                String coalitionId = coalitionIdForLoadBalancing;
                                //System.out.println("B1");
                                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionId);
                                //System.out.println("B2");
                                HostDescription destinationHost = dataCenterHostsInformation.get(coalitionId).get(new Random().nextInt(coalitionMembers.size()));
                                //System.out.println("B3");
                                System.out.println("DestinationHost " + destinationHost);

                                VirtualMachineDescription selectedVM = new VirtualMachineDescription();
                                if (hostDescription.getVirtualMachinesHosted().size() > 0) {
                                    selectedVM = hostDescription.getVirtualMachinesHosted().get(new Random().nextInt(hostDescription.getVirtualMachinesHosted().size()));
                                }

                                Decision decision = new Decision(hostDescription, destinationHost, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_A_TO_B);
                                ///////////////////////////////////////////////////////////////

                                agt.addBehaviour(new InitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_MEMORY, decision));
                                //agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_MEMORY, coalitionIdForLoadBalancing));
                            }
                        } else if ((thresholdViolationCounterForLowCPU >= Consts.MAX_THRESHOLD_VIOLATION_COUNTER_FOR_LOW_CPU) && Consts.BALANCE_CPU && (!hostDescription.isInProgress())) {
                            hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                            resetCounters();
                            resetAverageUsages();
                            lowCPUThresholdViolated = true;
                            /*
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * */
                            String coalitionIdForLoadBalancing = "";
                            coalitionIdForLoadBalancing = requestDecision();
                            agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingBtoA(agt, Consts.MIGRATION_CAUSE_LOW_CPU, coalitionIdForLoadBalancing));

                        } else if ((thresholdViolationCounterForLowMemory >= Consts.MAX_THRESHOLD_VIOLATION_COUNTER_FOR_LOW_MEMORY) && Consts.BALANCE_MEMORY && (!hostDescription.isInProgress())) {
                            hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                            resetCounters();
                            resetAverageUsages();
                            lowMemoryThresholdViolated = true;
                            /*
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                             * */
                            String coalitionIdForLoadBalancing = "";
                            coalitionIdForLoadBalancing = requestDecision();
                            agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingBtoA(agt, Consts.MIGRATION_CAUSE_LOW_MEMORY, coalitionIdForLoadBalancing));
                        }

                    } else if (Consts.MIGRATION_TRIGGER_TYPE == Consts.MIGRATION_TRIGGER_BASED_ON_AVERAGE_USAGE) {

                        currentTick++;

                        lastCPUUsages[currentTick] = hostDescription.getCPUUsage();
                        lastMemoryUsages[currentTick] = hostDescription.getMemoryUsage();

                        if (currentTick == (Consts.TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE - 1)) {

                            totalCPUUsage = 0;
                            totalMemoryUsage = 0;

                            for (int i = 0; i < Consts.TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE; i++) {
                                totalCPUUsage += lastCPUUsages[i];
                                totalMemoryUsage += lastMemoryUsages[i];
                            }

                            double averageCPUUsage = totalCPUUsage / (double) lastCPUUsages.length; // average CPU usage within a time window
                            double averageMemoryUsage = totalMemoryUsage / (double) lastMemoryUsages.length; // average Memory usage within a time window
                            if ((averageCPUUsage > hostDescription.getHighMigrationThresholdForCPU()) && (hostDescription.getVirtualMachinesHosted().size() > 0) && Consts.BALANCE_CPU && (!hostDescription.isInProgress())) {
                                hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                                currentTick = -1;
                                resetCounters();
                                resetAverageUsages();
                                highCPUThresholdViolated = true;

                                /*
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * */
                                String coalitionIdForLoadBalancing = "";
                                coalitionIdForLoadBalancing = requestDecision();

                                //System.out.println("I should start a VM migration from A to B");
                                if ((coalitionToHostAgents != null) && (coalitionLeaders != null) && (dataCenterHostsInformation != null)) {
                                    /////////// Randomly selecting a destination HostAgent and a VM.
                                    //System.out.println("C0");
                                    //String coalitionId = coalitionLeaders.get(new Random().nextInt(coalitionToHostAgents.size()));
                                    String coalitionId = coalitionIdForLoadBalancing;
                                            //System.out.println("C1");
                                    ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionId);
                                    //System.out.println("C2 " + coalitionId );
                                    //System.out.println(dataCenterHostsInformation.get(coalitionId));
                                    //System.out.println("C2.5 " + coalitionId );
                                    HostDescription destinationHost = dataCenterHostsInformation.get(coalitionId).get(new Random().nextInt(coalitionMembers.size()));
                                    //System.out.println("C3");
                                    //System.out.println("DestinationHost " + destinationHost);

                                    VirtualMachineDescription selectedVM = new VirtualMachineDescription();
                                    if (hostDescription.getVirtualMachinesHosted().size() > 0) {
                                        selectedVM = hostDescription.getVirtualMachinesHosted().get(new Random().nextInt(hostDescription.getVirtualMachinesHosted().size()));
                                    }

                                    Decision decision = new Decision(hostDescription, destinationHost, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_A_TO_B);
                                    ///////////////////////////////////////////////////////////////
                                    agt.addBehaviour(new InitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_CPU, decision));
                                    //agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_CPU, coalitionIdForLoadBalancing));
                                }

                            } else if ((averageMemoryUsage > hostDescription.getHighMigrationThresholdForMemory()) && Consts.BALANCE_MEMORY && (hostDescription.getVirtualMachinesHosted().size() > 0) && (!hostDescription.isInProgress())) {
                                hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                                currentTick = -1;
                                resetCounters();
                                resetAverageUsages();
                                highMemoryThresholdViolated = true;
                                /*
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * */

                                String coalitionIdForLoadBalancing = "";
                                coalitionIdForLoadBalancing = requestDecision();
                                if ((coalitionToHostAgents != null) && (coalitionLeaders != null) && (dataCenterHostsInformation != null)) {
                                /////////// Randomly selecting a destination HostAgent and a VM.
                                    //System.out.println("D0");
                                    //String coalitionId = coalitionLeaders.get(new Random().nextInt(coalitionToHostAgents.size()));
                                    String coalitionId = coalitionIdForLoadBalancing;
                                    //System.out.println("D1");
                                    ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionId);
                                    //System.out.println("D2");
                                    HostDescription destinationHost = dataCenterHostsInformation.get(coalitionId).get(new Random().nextInt(coalitionMembers.size()));
                                    //System.out.println("D3");
                                    //System.out.println("DestinationHost " + destinationHost);

                                VirtualMachineDescription selectedVM = new VirtualMachineDescription();
                                if (hostDescription.getVirtualMachinesHosted().size() > 0) {
                                    selectedVM = hostDescription.getVirtualMachinesHosted().get(new Random().nextInt(hostDescription.getVirtualMachinesHosted().size()));
                                }

                                Decision decision =  new Decision(hostDescription, destinationHost, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_A_TO_B);
                                ///////////////////////////////////////////////////////////////

                                agt.addBehaviour(new InitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_MEMORY, decision));
                                //agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingAtoB(agt, Consts.MIGRATION_CAUSE_HIGH_MEMORY, coalitionIdForLoadBalancing));

                                }

                            } else if ((averageCPUUsage < hostDescription.getLowMigrationThresholdForCPU()) && Consts.BALANCE_CPU && (!hostDescription.isInProgress())) {
                                hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                                currentTick = -1;
                                resetCounters();
                                resetAverageUsages();
                                lowCPUThresholdViolated = true;
                                /*
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * */
                                String coalitionIdForLoadBalancing = "";
                                coalitionIdForLoadBalancing = requestDecision();
                                agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingBtoA(agt, Consts.MIGRATION_CAUSE_LOW_CPU, coalitionIdForLoadBalancing));

                            } else if ((averageMemoryUsage < hostDescription.getLowMigrationThresholdForMemory()) && Consts.BALANCE_MEMORY && (!hostDescription.isInProgress())) {
                                hostDescription.setInProgress(true); // to be released once the load balancing algorithm has been executed.
                                currentTick = -1;
                                resetCounters();
                                resetAverageUsages();
                                lowMemoryThresholdViolated = true;
                                /*
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * Verify whether the agent will migrate the VM within its coalition or other external coalition.
                                 * */
                                String coalitionIdForLoadBalancing = "";
                                coalitionIdForLoadBalancing = requestDecision();
                                agt.addBehaviour(new CNPInitiatorForIntraLoadBalancingBtoA(agt, Consts.MIGRATION_CAUSE_LOW_MEMORY, coalitionIdForLoadBalancing));

                            }

                            if (currentTick == (Consts.TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE - 1)) {
                                currentTick = -1;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                hostDescription.setInProgress(false);
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 24" + ex);
                }
            }

        }

    }

    // Vector responses is a vector of <ACLMessage>. It can be iterated using an enumeration object -> Enumeration e = responses.elements()
    // Each ACLMessage contains a host description including its virtual machines. To access message content:
    // ACLMessage msg = (ACLMessage) e.nextElement();
    // (HostDescription)msg.getContentObject()
    private double mean(ArrayList<HostDescription> hosts, String resource) {
        double sum = 0.0;
        for (int i = 0; i < hosts.size(); i++) {
            HostDescription aHost = hosts.get(i);
            if (resource.toLowerCase().equals("cpu")) {
                sum += aHost.getCPUUsage();
            } else if (resource.toLowerCase().equals("memory")) {
                sum += aHost.getMemoryUsage();
            }
        }
        return sum / hosts.size();
    }

    private double stdDev(ArrayList<HostDescription> hosts, String resource) {
        double summatory = 0.0;
        double mean = mean(hosts, resource);
        for (int i = 0; i < hosts.size(); i++) {
            HostDescription aHost = hosts.get(i);
            if (resource.toLowerCase().equals("cpu")) {
                summatory += Math.pow(aHost.getCPUUsage() - mean, 2);
            } else if (resource.toLowerCase().equals("memory")) {
                summatory += Math.pow(aHost.getMemoryUsage() - mean, 2);
            }
        }
        return Math.sqrt(summatory / hosts.size());
    }

    private int[] calculateNewThresholds(ArrayList<HostDescription> hosts) {
        int[] thresholds = new int[4];
        // thresholds[0]  low CPU 
        // thresholds[1]  high CPU 
        // thresholds[2]  low Memory 
        // thresholds[3]  high Memory

        thresholds[0] = (int) Math.round(mean(hosts, "CPU")) - configuration.getTARGET_STD_DEV();
        thresholds[1] = (int) Math.round(mean(hosts, "CPU")) + configuration.getTARGET_STD_DEV();

        thresholds[2] = (int) Math.round(mean(hosts, "Memory")) - configuration.getTARGET_STD_DEV();
        thresholds[3] = (int) Math.round(mean(hosts, "Memory")) + configuration.getTARGET_STD_DEV();

        for (int i = 0; i < thresholds.length; i++) {
            if (thresholds[i] > 100) {
                thresholds[i] = 100;
            }
            if (thresholds[i] < 0) {
                thresholds[i] = 0;
            }
        }
        return thresholds;
    }


    private Decision selectHostAgentBasedOnCoalitionUtility(Vector responses, int loadBalancingCause) {
        Heuristics heuristics = new Heuristics(hostDescription, loadBalancingCause, responses, edges, configuration.getHEURISTIC());
        HostDescription selectedHost = heuristics.getSelectedHost();
        VirtualMachineDescription selectedVM = heuristics.getSelectedVM();

        try {
            // perform the migration
            switch (loadBalancingCause) {
                case Consts.MIGRATION_CAUSE_HIGH_CPU:
                case Consts.MIGRATION_CAUSE_HIGH_MEMORY:
                    if (selectedVM != null && selectedHost != null) {
                        if ((selectedVM.getNumberOfVirtualCores() <= selectedHost.getAvailableVirtualCores()) && (selectedVM.getMemory() <= selectedHost.getAvailableMemory())) {
                            return new Decision(this.hostDescription, selectedHost, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_A_TO_B);
                        } else {
                            System.out.println("WARNING. (AtoB) failed migration FROM " + hostDescription.getId() + " TO " + selectedHost.getId() + " WITH VM " + selectedVM.getId() + " and a valuation of " + heuristics.getValuationValue());
                            return new Decision(new HostDescription(), new HostDescription(), selectedVM, Consts.DECISION_TYPE_DONT_MIGRATE);
                        }

                    } else {
                        if (!Consts.LOG) {
                            System.out.println("ERROR: No host agent was selected because no one replied - Consts.MIGRATION_CAUSE_ = " + loadBalancingCause);
                        }
                        return new Decision(new HostDescription(), new HostDescription(), new VirtualMachineDescription(), Consts.DECISION_TYPE_DONT_MIGRATE);
                    }

                case Consts.MIGRATION_CAUSE_LOW_CPU:
                case Consts.MIGRATION_CAUSE_LOW_MEMORY:
                    if (selectedVM != null && selectedHost != null) {
                        if ((selectedVM.getNumberOfVirtualCores() <= hostDescription.getAvailableVirtualCores()) && (selectedVM.getMemory() <= hostDescription.getAvailableMemory())) {
                            return new Decision(selectedHost, this.hostDescription, selectedVM, Consts.DECISION_TYPE_MIGRATE_FROM_B_TO_A);
                        } else {
                            System.out.println("WARNING. (BtoA) failed migration FROM " + selectedHost.getId() + " TO " + hostDescription.getId() + " THE VM " + selectedVM.getId() + " and a valuation of " + heuristics.getValuationValue());
                            return new Decision(new HostDescription(), new HostDescription(), selectedVM, Consts.DECISION_TYPE_DONT_MIGRATE);
                        }
                    } else { // External host agents do not have VMs to migrate, so no load balancing is possible.
                        if (!Consts.LOG) {
                            System.out.println("ERROR: No host agent was selected because no one replied - Consts.MIGRATION_CAUSE_ = " + loadBalancingCause);
                        }
                        return new Decision(new HostDescription(), new HostDescription(), new VirtualMachineDescription(), Consts.DECISION_TYPE_DONT_MIGRATE);
                    }

                default:
                    if (!Consts.LOG) {
                        System.out.println("Error: Unknown load balancing cause");
                    }
                    return null;
            }
        } catch (Exception ex) {
            if (!Consts.LOG) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
            ex.printStackTrace();
        }
        if (!Consts.LOG) {
            System.out.println("Error: For some reason, no agent was selected. Load balancing cause " + loadBalancingCause);
        }
        return null; // if no agent was selected for any reason return a null host description. This will reject all the participant agents' proposals. 
    }


    private class CNPInitiatorForIntraLoadBalancingAtoB extends OneShotBehaviour { // Initiator of Contract Net Protocol for VM Migration

        private Agent agt;
        private Decision decision;
        private int loadBalancingCause;
        private int numberOfPotentialRespondents;
        private ACLMessage callForProposalsForLoadBalancing;
        private String coalitionIdForLoadBalancing;

        public CNPInitiatorForIntraLoadBalancingAtoB(Agent agt, int loadBalancingCause, String coalitionIdForLoadBalancing) {
            super(null);
            this.loadBalancingCause = loadBalancingCause;
            this.agt = agt;
            this.decision = new Decision();
            this.coalitionIdForLoadBalancing = coalitionIdForLoadBalancing;
        }

        @Override
        public void action() {
            try {
                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionIdForLoadBalancing);
                callForProposalsForLoadBalancing = new ACLMessage(ACLMessage.CFP);
                numberOfPotentialRespondents = 0;
                for (int i = 0; i < coalitionMembers.size(); i++) {
                    if (!coalitionMembers.get(i).equals(hostDescription.getId())) {
                        callForProposalsForLoadBalancing.addReceiver(new AID(coalitionMembers.get(i), AID.ISLOCALNAME));
                        numberOfPotentialRespondents++;
                    }
                }


                callForProposalsForLoadBalancing.setSender(agt.getAID());
                callForProposalsForLoadBalancing.setConversationId(Consts.CONVERSATION_LOAD_BALANCING_A_TO_B);
                callForProposalsForLoadBalancing.setContent(String.valueOf(loadBalancingCause));

                callForProposalsForLoadBalancing.setReplyWith(String.valueOf(agt.getLocalName() + "-" + String.valueOf(conversationId)));
                callForProposalsForLoadBalancing.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                callForProposalsForLoadBalancing.setReplyByDate(new Date(System.currentTimeMillis() + Consts.TIMEOUT_FOR_CNP_INITIATOR_FOR_LOAD_BALANCING));
                if (!Consts.LOG) {
                    System.out.println("****** Initiator of CNP for intra load balancing from A to B " + agt.getLocalName());
                }
                conversationId++;

                addBehaviour(new ContractNetInitiator(agt, callForProposalsForLoadBalancing) {
                    @Override
                    protected void handleNotUnderstood(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleOutOfSequence(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleRefuse(ACLMessage refuse) {
                        if (!Consts.LOG) {
                            System.out.println(refuse.getSender().getName() + " refused to participate for some reason");
                        }
                    }

                    @Override
                    protected void handleFailure(ACLMessage failure) {
                        hostDescription.setInProgress(false);
                        if (failure.getSender().equals(myAgent.getAMS())) {
                            if (!Consts.LOG) {
                                System.out.println("Respondent does not exist");
                            }
                        } else {
                            if (!Consts.LOG) {
                                System.out.println(failure.getSender().getName() + " failed");
                            }
                        }
                        numberOfPotentialRespondents--;
                    }

                    @Override
                    protected void handleAllResponses(Vector responses, Vector acceptances) {
                        if (responses.size() < numberOfPotentialRespondents) {
                            if (!Consts.LOG) {
                                System.out.println(agt.getName() + " - Timeout expired: missing " + (numberOfPotentialRespondents - responses.size()) + " responses");
                            }
                        }

                        // Filter out all the responses from all the host unwilling or unable to participate
                        int k = 0;
                        Vector responsesFromNotFailedHAs = new Vector<HostDescription>();
                        Vector responsesFromWillingNotFailedHAs = new Vector<HostDescription>();
                        while (k < responses.size()) {// responses from all the (PARTICIPANT) hosts
                            try {
                                HostDescription participantHost = (HostDescription) ((ACLMessage) responses.get(k)).getContentObject();
                                if (!participantHost.isFailed()) {
                                    //participantHost.getId()+" was added to NotFailed"
                                    responsesFromNotFailedHAs.add(responses.get(k));
                                    if (participantHost.isWillingToParticipateInCNP()) {
                                        //participantHost.getId()+" was also added to WillingToParticipate"
                                        responsesFromWillingNotFailedHAs.add(responses.get(k));
                                    }
                                }
                            } catch (UnreadableException ex) {
                                if (Consts.EXCEPTIONS) {
                                    System.out.println(ex);
                                }
                            }
                            k++;
                        }


                        if (responses.size() > 0) {

                            boolean proposalAccepted = false;

                            if (responsesFromWillingNotFailedHAs.size() > 0) {
                                decision = selectHostAgentBasedOnCoalitionUtility(responsesFromWillingNotFailedHAs, loadBalancingCause);
                            } else {
                                decision = new Decision();
                            }

                            int[] thresholds = {0, configuration.getTARGET_STD_DEV(), 0, configuration.getTARGET_STD_DEV()};

                            if (responsesFromNotFailedHAs.size() > 0) {
                                //thresholds = calculateNewThresholds(responsesFromNotFailedHAs);
                            }
                            // thresholds[0]  low CPU
                            decision.setLowMigrationThresholdForCPU(thresholds[0]);
                            // thresholds[1]  high CPU
                            decision.setHighMigrationThresholdForCPU(thresholds[1]);
                            // thresholds[2]  low Memory
                            decision.setLowMigrationThresholdForMemory(thresholds[2]);
                            // thresholds[3]  high Memory
                            decision.setHighMigrationThresholdForMemory(thresholds[3]);

                            if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                                hostDescription.setLowMigrationThresholdForCPU(thresholds[0]);
                                hostDescription.setHighMigrationThresholdForCPU(thresholds[1]);
                                hostDescription.setLowMigrationThresholdForMemory(thresholds[2]);
                                hostDescription.setHighMigrationThresholdForMemory(thresholds[3]);
                            }


                            ACLMessage msg;
                            ACLMessage reply;
                            for (int i = 0; i < responses.size(); i++) {
                                msg = (ACLMessage) responses.get(i);
                                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                    reply = msg.createReply();
                                    if (msg.getSender().getLocalName().equals(decision.getDestinationHost().getId())) {
                                        try {
                                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                                            //reply.setContent(agt.getContainerController().getContainerName());
                                            reply.setContentObject(decision);
                                            acceptances.addElement(reply);
                                            proposalAccepted = true;
                                            if (!Consts.LOG) {
                                                System.out.println("ACCEPT - " + msg.getSender().getLocalName() + " = " + decision.getDestinationHost().getId());
                                            }
                                        } catch (Exception ex) {
                                            if (Consts.EXCEPTIONS) {
                                                System.out.println("It is here 27" + ex);
                                            }
                                        }
                                    } else {
                                        try {
                                            if (!Consts.LOG) {
                                                System.out.println("REJECT - " + msg.getSender().getLocalName() + " = " + decision.getDestinationHost().getId());
                                            }
                                            reply.setContentObject(decision);
                                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                            acceptances.addElement(reply);
                                        } catch (IOException ex) {
                                            if (Consts.EXCEPTIONS) {
                                                System.out.println("Is it here 2" + ex);
                                            }
                                        }
                                    }
                                }
                            }


                            if (proposalAccepted) {
                                if (!Consts.LOG) {
                                    System.out.println("Agent " + decision.getDestinationHost().getId() + " was selected for Load Balancing from A to B. Load balancing cause " + loadBalancingCause);
                                }

                            } else { // if the VM was not accepted for any member of coalition, unlock it and start inter_load balancing if enabled.
                                if (!Consts.LOG) {
                                    System.out.println("No agent was selected for Intra Load Balancing from A to B. Load balancing cause " + loadBalancingCause);
                                }
                                if (!Consts.LOG) {
                                    System.out.println("The decision was " + decision.getDecision());
                                }

                                // just clean up and reset thresholds
                                resetAverageUsages();
                                resetCounters();
                                resetThresholdFlags();
                                hostDescription.setInProgress(false);

                            }
                        } else { // if no agent replied to the cfp, unlock vm
                            if (!Consts.LOG) {
                                System.out.println("No agent replied to cfp. Load balancing cause " + loadBalancingCause);
                            }
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }

                    }

                    @Override
                    protected void handleInform(ACLMessage inform) { // I'll use this as an acknowledge from the selected hostAgent, once it acknowledges that the VM has been accepted and that there are sufficient resources to host it, I can remove the vm

                        if ((loadBalancingCause == Consts.MIGRATION_CAUSE_HIGH_CPU) || (loadBalancingCause == Consts.MIGRATION_CAUSE_HIGH_MEMORY)) { // it means this HostAgent will migrate one of his VMs to other HostAgents
                            if (!Consts.LOG) {
                                System.out.println("Agent " + inform.getSender().getName() + " confirms that it will host the VM and that sufficient resources have been allocated");
                            }
                            decision.getSelectedVM().setContainerName(inform.getContent());
                            decision.getSelectedVM().setPreviousOwnerId(hostDescription.getId());
                            decision.getSelectedVM().setOwnerId(inform.getSender().getLocalName());
                            operationOverVM(decision.getSelectedVM(), "removeAndMigrate", "AtoB", null, null);
                            if (configuration.isBALANCING_ONLY_ONE_COALITION_AT_A_TIME())
                                agt.addBehaviour(new ResetDatacenterLoadBalancingCounters(agt));
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                        } else {
                            if (!Consts.LOG) {
                                System.out.println("ERROR: Unknown load balancing cause");
                            }
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }

                    }

                });

            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here" + ex);
                }
            }

        }

    }


    private class InitiatorForIntraLoadBalancingAtoB extends OneShotBehaviour { // Initiator of Contract Net Protocol for VM Migration

        private Agent agt;
        private Decision decision;
        private int loadBalancingCause;
        private int numberOfPotentialRespondents;
        private ACLMessage ProposalForLoadBalancing;

        public InitiatorForIntraLoadBalancingAtoB(Agent agt, int loadBalancingCause, Decision decision) {
            super(null);
            this.loadBalancingCause = loadBalancingCause;
            this.agt = agt;
            this.decision = decision;

        }

        @Override
        public void action() {
            try {

                ProposalForLoadBalancing = new ACLMessage(ACLMessage.PROPOSE);
                numberOfPotentialRespondents = 1;
                ProposalForLoadBalancing.setSender(agt.getAID());
                ProposalForLoadBalancing.setConversationId(Consts.CONVERSATION_LOAD_BALANCING_A_TO_B);
                ProposalForLoadBalancing.setContentObject(String.valueOf(loadBalancingCause));
                ProposalForLoadBalancing.setReplyWith(String.valueOf(agt.getLocalName() + "-" + String.valueOf(conversationId)));
                ProposalForLoadBalancing.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
                ProposalForLoadBalancing.setReplyByDate(new Date(System.currentTimeMillis() + Consts.TIMEOUT_FOR_CNP_INITIATOR_FOR_LOAD_BALANCING));

                ProposalForLoadBalancing.addReceiver(new AID(decision.getDestinationHost().getId(), AID.ISLOCALNAME));

                //System.out.println("****** Initiator for intra load balancing from A to B " + agt.getLocalName()+ " " + decision.getSourceHost().getId() + " " + decision.getDestinationHost().getId()+ " " + decision.getSelectedVM().getId());

                conversationId++;

                addBehaviour(new ProposeInitiator(agt, ProposalForLoadBalancing) {
                    @Override
                    protected void handleNotUnderstood(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleOutOfSequence(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleRejectProposal(ACLMessage rejection) {
                        resetAverageUsages();
                        resetCounters();
                        resetThresholdFlags();
                        hostDescription.setInProgress(false);
                        //System.out.println(hostDescription.getId() + "Thanks anyway");
                    }

                    @Override
                    protected void handleAcceptProposal(ACLMessage acceptance) {
                        //System.out.println(hostDescription.getId() + " Thanks for accepting");
                        if ((loadBalancingCause == Consts.MIGRATION_CAUSE_HIGH_CPU) || (loadBalancingCause == Consts.MIGRATION_CAUSE_HIGH_MEMORY)) { // it means this HostAgent will migrate one of his VMs to other HostAgents
                            //System.out.println("Agent " + acceptance.getSender().getName() + " just confirmed that it will host the VM and that sufficient resources have been allocated");
                            decision.getSelectedVM().setContainerName(acceptance.getContent());
                            decision.getSelectedVM().setPreviousOwnerId(hostDescription.getId());
                            decision.getSelectedVM().setOwnerId(acceptance.getSender().getLocalName());
                            //System.out.println();
                            operationOverVM(decision.getSelectedVM(), "removeAndMigrate", "AtoB", null, null);
                            if (configuration.isBALANCING_ONLY_ONE_COALITION_AT_A_TIME())
                                agt.addBehaviour(new ResetDatacenterLoadBalancingCounters(agt));
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                        } else {
                            if (!Consts.LOG) {
                                System.out.println("ERROR: Unknown load balancing cause");
                            }
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }
                    }

                    @Override
                    protected void handleAllResponses(Vector responses) {
                        if (responses.size() < numberOfPotentialRespondents) {
                            System.out.println(agt.getName() + " --- Timeout expired: missing " + (numberOfPotentialRespondents - responses.size()) + " responses");
                            System.out.println(hostDescription.getId() + " handleAllResponses has been executed");
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }
                    }


                });

            } catch (Exception ex) {
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here" + ex);
                }
            }

        }

    }

    private class CNPInitiatorForIntraLoadBalancingBtoA extends OneShotBehaviour { // Initiator of Contract Net Protocol for VM Migration

        private Agent agt;
        private Decision decision;
        private int loadBalancingCause;
        private int numberOfPotentialRespondents;
        private String coalitionIdForLoadBalancing;

        public CNPInitiatorForIntraLoadBalancingBtoA(Agent agt, int loadBalancingCause, String coalitionIdForLoadBalancing) {
            super(null);
            this.loadBalancingCause = loadBalancingCause;
            this.agt = agt;
            this.decision = new Decision();
            this.coalitionIdForLoadBalancing = coalitionIdForLoadBalancing;
        }

        @Override
        public void action() {
            try {

                ArrayList<String> coalitionMembers = coalitionToHostAgents.get(coalitionIdForLoadBalancing);
                ACLMessage callForProposalsForLoadBalancing = new ACLMessage(ACLMessage.CFP);
                numberOfPotentialRespondents = 0;
                for (int i = 0; i < coalitionMembers.size(); i++) {
                    if (!coalitionMembers.get(i).equals(hostDescription.getId())) {
                        callForProposalsForLoadBalancing.addReceiver(new AID(coalitionMembers.get(i), AID.ISLOCALNAME));
                        numberOfPotentialRespondents++;
                    }
                }
                callForProposalsForLoadBalancing.setSender(agt.getAID());
                callForProposalsForLoadBalancing.setConversationId(Consts.CONVERSATION_LOAD_BALANCING_B_TO_A);
                callForProposalsForLoadBalancing.setContent(String.valueOf(loadBalancingCause));

                callForProposalsForLoadBalancing.setReplyWith(String.valueOf(agt.getLocalName() + "-" + String.valueOf(conversationId)));
                callForProposalsForLoadBalancing.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                callForProposalsForLoadBalancing.setReplyByDate(new Date(System.currentTimeMillis() + Consts.TIMEOUT_FOR_CNP_INITIATOR_FOR_LOAD_BALANCING));

                conversationId++;

                addBehaviour(new ContractNetInitiator(agt, callForProposalsForLoadBalancing) {
                    @Override
                    protected void handleNotUnderstood(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleOutOfSequence(ACLMessage notUnderstood) {
                        hostDescription.setInProgress(false);
                    }

                    @Override
                    protected void handleRefuse(ACLMessage refuse) {
                        if (!Consts.LOG) {
                            System.out.println(refuse.getSender().getName() + " refused to participate for some reason");
                        }
                    }

                    @Override
                    protected void handleFailure(ACLMessage failure) {
                        hostDescription.setInProgress(false);
                        if (failure.getSender().equals(myAgent.getAMS())) {
                            if (!Consts.LOG) {
                                System.out.println("Respondent does not exist");
                            }
                        } else {
                            if (!Consts.LOG) {
                                System.out.println(failure.getSender().getName() + " failed");
                            }
                        }
                        numberOfPotentialRespondents--;
                    }

                    @Override
                    protected void handleAllResponses(Vector responses, Vector acceptances) {
                        if (responses.size() < numberOfPotentialRespondents) {
                            if (!Consts.LOG) {
                                System.out.println(agt.getName() + " - Timeout expired: missing " + (numberOfPotentialRespondents - responses.size()) + " responses");
                            }
                        }

                        // Filter out all the responses from all the host unwilling or unable to participate
                        int k = 0;
                        Vector responsesFromNotFailedHAs = new Vector<HostDescription>();
                        Vector responsesFromWillingNotFailedHAs = new Vector<HostDescription>();
                        while (k < responses.size()) {// responses from all the (PARTICIPANT) hosts
                            try {
                                HostDescription participantHost = (HostDescription) ((ACLMessage) responses.get(k)).getContentObject();
                                if (!participantHost.isFailed()) {
                                    //participantHost.getId()+" was added to NotFailed"
                                    responsesFromNotFailedHAs.add(responses.get(k));
                                    if (participantHost.isWillingToParticipateInCNP()) {
                                        //participantHost.getId()+" was also added to WillingToParticipate";
                                        responsesFromWillingNotFailedHAs.add(responses.get(k));
                                    }
                                }
                            } catch (UnreadableException ex) {
                                if (Consts.EXCEPTIONS) {
                                    System.out.println(ex);
                                }
                            }
                            k++;
                        }


                        if (responses.size() > 0) {

                            boolean proposalAccepted = false;

                            if (responsesFromWillingNotFailedHAs.size() > 0) {
                                decision = selectHostAgentBasedOnCoalitionUtility(responsesFromWillingNotFailedHAs, loadBalancingCause);
                            } else {
                                decision = new Decision();
                            }

                            int[] thresholds = {0, configuration.getTARGET_STD_DEV(), 0, configuration.getTARGET_STD_DEV()};

                            if (responsesFromNotFailedHAs.size() > 0) {
                                //thresholds = calculateNewThresholds(responsesFromNotFailedHAs);
                            }
                            decision.setLowMigrationThresholdForCPU(thresholds[0]);
                            decision.setHighMigrationThresholdForCPU(thresholds[1]);
                            decision.setLowMigrationThresholdForMemory(thresholds[2]);
                            decision.setHighMigrationThresholdForMemory(thresholds[3]);

                            if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                                hostDescription.setLowMigrationThresholdForCPU(thresholds[0]);
                                hostDescription.setHighMigrationThresholdForCPU(thresholds[1]);
                                hostDescription.setLowMigrationThresholdForMemory(thresholds[2]);
                                hostDescription.setHighMigrationThresholdForMemory(thresholds[3]);
                            }


                            ACLMessage msg;
                            ACLMessage reply;
                            for (int i = 0; i < responses.size(); i++) {
                                msg = (ACLMessage) responses.get(i);
                                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                    reply = msg.createReply();
                                    if (msg.getSender().getLocalName().equals(decision.getSourceHost().getId())) {
                                        try {
                                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                                            //reply.setContent(agt.getContainerController().getContainerName());
                                            reply.setContentObject(decision);
                                            //System.out.println("A PROPOSAL WAS ACCEPTED");
                                            acceptances.addElement(reply);
                                            proposalAccepted = true;
                                            if (!Consts.LOG) {
                                                System.out.println("ACCEPT - " + msg.getSender().getLocalName() + " = " + decision.getSourceHost().getId());
                                            }
                                        } catch (Exception ex) {
                                            if (Consts.EXCEPTIONS) {
                                                System.out.println("It is here 27" + ex);
                                            }
                                        }
                                    } else {
                                        try {
                                            if (!Consts.LOG) {
                                                System.out.println("REJECT - " + msg.getSender().getLocalName() + " = " + decision.getSourceHost().getId());
                                            }
                                            reply.setContentObject(decision);
                                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                            //System.out.println("A PROPOSAL WAS REJECTED");
                                            acceptances.addElement(reply);
                                        } catch (IOException ex) {
                                            if (Consts.EXCEPTIONS) {
                                                System.out.println("Is it here 2" + ex);
                                            }
                                        }
                                    }
                                }
                            }

                            if (proposalAccepted) {
                                if (!Consts.LOG) {
                                    System.out.println("Agent " + decision.getSourceHost().getId() + " was selected for Load Balancing from B to A. Load balancing cause " + loadBalancingCause);
                                }

                            } else { // if the VM was not accepted for any member of coalition, unlock it and start inter_load balancing if enabled.
                                if (!Consts.LOG) {
                                    System.out.println("No agent was selected for Intra Load Balancing from B to A. Load balancing cause " + loadBalancingCause);
                                }
                                if (!Consts.LOG) {
                                    System.out.println("The decision was " + decision.getDecision());
                                }

                                // if just clean up and reset thresholds
                                resetAverageUsages();
                                resetCounters();
                                resetThresholdFlags();
                                hostDescription.setInProgress(false);
                            }
                        } else { // if no agent replied to the cfp, unlock vm
                            if (!Consts.LOG) {
                                System.out.println("No agent replied to cfp. Load balancing cause " + loadBalancingCause);
                            }
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }

                    }

                    @Override
                    protected void handleInform(ACLMessage inform) { // I'll use this as an acknowledge from the selected hostAgent, once it acknowledges that the VM has been accepted and that there are sufficient resources to host it, I can remove the vm
                        if ((loadBalancingCause == Consts.MIGRATION_CAUSE_LOW_CPU) || (loadBalancingCause == Consts.MIGRATION_CAUSE_LOW_MEMORY)) {
                            if (!Consts.LOG) {
                                System.out.println("Agent " + inform.getSender().getName() + " confirms that it will send the VM");
                            }

                        } else {
                            if (!Consts.LOG) {
                                System.out.println("ERROR: Unknown load balancing cause");
                            }
                            resetAverageUsages();
                            resetCounters();
                            resetThresholdFlags();
                            hostDescription.setInProgress(false);
                        }

                    }

                });

            } catch (Exception ex) {
                hostDescription.setInProgress(false);
                if (Consts.EXCEPTIONS) {
                    System.out.println("It is here 30" + ex);
                }
            }

        }

    }

    private class CNPParticipantForIntraLoadBalancingBtoA extends OneShotBehaviour {

        private Agent agt;

        public CNPParticipantForIntraLoadBalancingBtoA(Agent agt) {
            this.agt = agt;
        }

        @Override
        public synchronized void action() {
            if (!Consts.LOG) {
                System.out.println("Agent " + getLocalName() + " waiting for CFP for Intra Load Balancing from B to A ...");
            }

            MessageTemplate subtemplate = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET), MessageTemplate.MatchConversationId(Consts.CONVERSATION_LOAD_BALANCING_B_TO_A));
            MessageTemplate template = MessageTemplate.and(subtemplate, MessageTemplate.MatchPerformative(ACLMessage.CFP));

            addBehaviour(new ContractNetResponder(agt, template) {
                @Override
                protected void handleOutOfSequence(ACLMessage msg) {
                    hostDescription.setInProgress(false);
                }

                @Override
                protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                    resetAverageUsages();
                    resetCounters();
                    return handleCallForProposals(agt, cfp);  // handleCallForProposals sets inProgress to true;
                }

                @Override
                protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                    ACLMessage inform = accept.createReply();
                    try {
                        inform.setPerformative(ACLMessage.INFORM);
                        if (!Consts.LOG) {
                            System.out.println("Agent " + getLocalName() + " accept proposal from Agent " + cfp.getSender());
                        }

                        inform.setContent(agt.getContainerController().getContainerName());
                        Decision decision = (Decision) accept.getContentObject();

                        decision.getSelectedVM().setContainerName(decision.getDestinationHost().getContainerName());
                        decision.getSelectedVM().setPreviousOwnerId(hostDescription.getId());
                        decision.getSelectedVM().setOwnerId(accept.getSender().getLocalName());


                        operationOverVM(decision.getSelectedVM(), "removeAndMigrate", "BtoA", null, null);
                        if (configuration.isBALANCING_ONLY_ONE_COALITION_AT_A_TIME())
                            agt.addBehaviour(new ResetDatacenterLoadBalancingCounters(agt));
                        resetAverageUsages();
                        resetCounters();
                        resetThresholdFlags();

                        if (!Consts.LOG) {
                            System.out.println("I " + agt.getAID() + " updated his migration thresholds because of acceptance BtoA");
                        }
                        if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                            hostDescription.setLowMigrationThresholdForCPU(decision.getLowMigrationThresholdForCPU());
                            hostDescription.setHighMigrationThresholdForCPU(decision.getHighMigrationThresholdForCPU());
                            hostDescription.setLowMigrationThresholdForMemory(decision.getLowMigrationThresholdForMemory());
                            hostDescription.setHighMigrationThresholdForMemory(decision.getHighMigrationThresholdForMemory());
                        }

                    } catch (Exception ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 31" + ex);
                        }
                    }
                    return inform;
                }

                @Override
                protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                    try {
                        Decision decision = (Decision) reject.getContentObject();

                        if (!failed) {
                            if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                                hostDescription.setLowMigrationThresholdForCPU(decision.getLowMigrationThresholdForCPU());
                                hostDescription.setHighMigrationThresholdForCPU(decision.getHighMigrationThresholdForCPU());
                                hostDescription.setLowMigrationThresholdForMemory(decision.getLowMigrationThresholdForMemory());
                                hostDescription.setHighMigrationThresholdForMemory(decision.getHighMigrationThresholdForMemory());
                            }
                        } else {
                            hostDescription.setLowMigrationThresholdForCPU(0);
                            hostDescription.setHighMigrationThresholdForCPU(configuration.getTARGET_STD_DEV());
                            hostDescription.setLowMigrationThresholdForMemory(0);
                            hostDescription.setHighMigrationThresholdForMemory(configuration.getTARGET_STD_DEV());
                        }

                        if (!Consts.LOG) {
                            System.out.println("I " + agt.getAID() + " updated his migration thresholds because of rejection BtoA");
                        }
                        if (!Consts.LOG) {
                            System.out.println("Agent " + getLocalName() + " got proposal rejected");
                        }
                        resetAverageUsages();
                        resetCounters();
                        resetThresholdFlags();
                        hostDescription.setInProgress(false); // if proposal rejected release the agent so it can participate in other CNPs
                    } catch (Exception ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 32: " + ex);
                        }
                    }
                }
            });
        }
    }

    private class CNPParticipantForIntraLoadBalancingAtoB extends OneShotBehaviour {

        private Agent agt;

        public CNPParticipantForIntraLoadBalancingAtoB(Agent agt) {
            this.agt = agt;
        }

        @Override
        public synchronized void action() {
            if (!Consts.LOG) {
                System.out.println("Agent " + getLocalName() + " waiting for CFP for Intra Load Balancing from A to B ...");
            }

            MessageTemplate subtemplate = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET), MessageTemplate.MatchConversationId(Consts.CONVERSATION_LOAD_BALANCING_A_TO_B));
            MessageTemplate template = MessageTemplate.and(subtemplate, MessageTemplate.MatchPerformative(ACLMessage.CFP));

            addBehaviour(new ContractNetResponder(agt, template) {

                @Override
                protected void handleOutOfSequence(ACLMessage msg) {
                    hostDescription.setInProgress(false);
                }

                @Override
                protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                    resetAverageUsages();
                    resetCounters();
                    return handleCallForProposals(agt, cfp);  // handleCallForProposals sets inProgress to true; 
                }

                @Override
                protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                    ACLMessage inform = accept.createReply();
                    try {

                        Decision decision = (Decision) accept.getContentObject();

                        inform.setPerformative(ACLMessage.INFORM);
                        if (!Consts.LOG) {
                            System.out.println("Agent " + getLocalName() + " accept proposal from Agent " + cfp.getSender());
                        }
                        inform.setContent(agt.getContainerController().getContainerName());
                        if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                            hostDescription.setLowMigrationThresholdForCPU(decision.getLowMigrationThresholdForCPU());
                            hostDescription.setHighMigrationThresholdForCPU(decision.getHighMigrationThresholdForCPU());
                            hostDescription.setLowMigrationThresholdForMemory(decision.getLowMigrationThresholdForMemory());
                            hostDescription.setHighMigrationThresholdForMemory(decision.getHighMigrationThresholdForMemory());
                        }
                        if (!Consts.LOG) {
                            System.out.println("I " + agt.getAID() + " updated his migration thresholds because of acceptance AtoB");
                        }
                    } catch (Exception ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 4: " + ex);
                        }
                    }
                    return inform;
                }

                @Override
                protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                    try {
                        Decision decision = (Decision) reject.getContentObject();

                        if (!failed) {
                            if (decision.getSourceHost().getCoalition() == decision.getDestinationHost().getCoalition()) {
                                hostDescription.setLowMigrationThresholdForCPU(decision.getLowMigrationThresholdForCPU());
                                hostDescription.setHighMigrationThresholdForCPU(decision.getHighMigrationThresholdForCPU());
                                hostDescription.setLowMigrationThresholdForMemory(decision.getLowMigrationThresholdForMemory());
                                hostDescription.setHighMigrationThresholdForMemory(decision.getHighMigrationThresholdForMemory());
                            }
                        } else {
                            hostDescription.setLowMigrationThresholdForCPU(0);
                            hostDescription.setHighMigrationThresholdForCPU(configuration.getTARGET_STD_DEV());
                            hostDescription.setLowMigrationThresholdForMemory(0);
                            hostDescription.setHighMigrationThresholdForMemory(configuration.getTARGET_STD_DEV());
                        }

                        if (!Consts.LOG) {
                            System.out.println("I " + agt.getAID() + " updated his migration thresholds because of rejection AtoB");
                        }
                        if (!Consts.LOG) {
                            System.out.println("Agent " + getLocalName() + " got proposal rejected");
                        }
                        resetAverageUsages();
                        resetCounters();
                        resetThresholdFlags();
                        hostDescription.setInProgress(false); // if proposal rejected release the agent so it can participate in other CNPs
                    } catch (Exception ex) {
                        if (Consts.EXCEPTIONS) {
                            System.out.println("It is here 5: " + ex);
                        }
                    }
                }
            });
        }
    }
    private class ParticipantForIntraLoadBalancingAtoB extends OneShotBehaviour {

        private Agent agt;

        public ParticipantForIntraLoadBalancingAtoB(Agent agt) {
            this.agt = agt;
        }

        @Override
        public synchronized void action() {
            //System.out.println("Agent " + getLocalName() + " waiting for Proposal for Intra Load Balancing from A to B ...");

            MessageTemplate subtemplate = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE), MessageTemplate.MatchConversationId(Consts.CONVERSATION_LOAD_BALANCING_A_TO_B));
            MessageTemplate template = MessageTemplate.and(subtemplate, MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

            addBehaviour(new ProposeResponder(agt, template) {
                @Override
                protected ACLMessage prepareResponse(ACLMessage propose) {
                    ACLMessage result = propose.createReply();
                    if (Math.random() > 0.5) {
                        result.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        result.setContent("Hi" + propose.getSender() + " from " + hostDescription.getId());
                        //System.out.println("-------- ACCEPTING "  + "Hi" + propose.getSender() + " from " + hostDescription.getId());
                    } else {
                        result.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        result.setContent("Hi" + propose.getSender() + " from " + hostDescription.getId());
                        //System.out.println("--------REJECTING "  + "Hi" + propose.getSender() + " from " + hostDescription.getId());
                    }
                    return result;
                }
            });
        }
    }

    private synchronized ACLMessage handleCallForProposals(Agent agt, ACLMessage cfp) {
        ACLMessage result = null;
        try {
            result = cfp.createReply();
            result.setPerformative(ACLMessage.PROPOSE);
            if (!hostDescription.isInProgress() && !failed) {
                hostDescription.setInProgress(true);
                hostDescription.setWillingToParticipateInCNP(true);
            } else {// If there are already some locked/compromised resources or the host has failed, simply refuse to participate in CFPs.
                hostDescription.setWillingToParticipateInCNP(false);
            }
            result.setContentObject(hostDescription);
        } catch (Exception ex) {
            if (Consts.EXCEPTIONS) {
                System.out.println("Is it here 6" + ex);
            }
        }
        return result;
    }
}
