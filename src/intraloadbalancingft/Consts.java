/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intraloadbalancingft;
import java.util.stream.IntStream;
import java.util.Collections;

/**
 * @author octavio
 */
public final class Consts {
    public static final int DISTRIBUTED_FIXED_COALITIONS = 0;
    public static final int VMWARE_CENTRALIZED_WITH_NO_COALITIONS = 1;

    public static final String EXHAUSTIVE = "EXHAUSTIVE";
    public static final String MAXMIN = "MAXMIN";
    public static final String ROULETTE = "ROULETTE";

    ////// **** Constants for Workload Generation **** ///////
    public static final int AVG_INTERARRIVAL_TIME = 50; // in ms
    public static final int AVG_INTERDEPARTURE_TIME = 25000; // in ms


//    public static final int AVG_INTERDEPARTURE_TIME = 20000; // in ms
//    public static final int NUMBER_OF_VMNUMBER_OF_VMSS = 20000;

//    public static final int AVG_INTERDEPARTURE_TIME = 25000; // in ms
//    public static final int NUMBER_OF_VMS = 30000;

//    public static final int AVG_INTERDEPARTURE_TIME = 600000; // in ms
//    public static final int NUMBER_OF_VMS = 10000;

    ////// **** Constants for Agent Platform **** ///////    

    public static final int NUMBER_OF_LOG_RECORDS = 1000; // after NUMBER_OF_LOG_RECORDS the system stops.


    public static final int MIGRATION_TRIGGER_BASED_ON_COUNTERS = 0; // how many times a given resource usage has violated a threshold within a moving time window
    public static final int MIGRATION_TRIGGER_BASED_ON_AVERAGE_USAGE = 1; // average resource usage above/below  threshold within a moving time window
    public static final int MIGRATION_TRIGGER_TYPE = MIGRATION_TRIGGER_BASED_ON_AVERAGE_USAGE;
    public static final long HOST_REPORTING_RATE = 1000; // in ms
    public static final long RANGE_OF_RANDOM_TICKS = HOST_REPORTING_RATE / 2; // This is to prevent Host Agents from initiating CNPs at the same time.


    public static final long LOGGING_RATE = 1000; // in ms

    //Usable port numbers range from 1000 to 65,535


    ////// **** Constants for VMware load balancing mechanism  **** ///////

    // When comparing VMware against our work, VMWARE_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE == HOST_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE
    //public static final int VMWARE_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE = 10;     // For a HOST_REPORTING_RATE of 1000 ms this means that the time window is 10 seconds
    // When comparing VMware against our work, VMWARE_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE == HOST_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE


    //public static final int VMWARE_MAX_MIGRATIONS = 10000;

    public static final boolean VMWARE_BALANCE_CPU_LOAD = true;
    public static final boolean VMWARE_BALANCE_MEMORY_LOAD = false;

    public static final String VMWARE_CONVERSATION_VM_MIGRATION = "VMWARE_CONVERSATION_VM_MIGRATION";
    public static final String VMWARE_CONVERSATION_LOCK_VM = "VMWARE_CONVERSATION_LOCK_VM";
    public static final String VMWARE_CONVERSATION_LOCK_RESOURCES = "VMWARE_CONVERSATION_LOCK_RESOURCES";
    public static final String VMWARE_CONVERSATION_UNLOCK = "VMWARE_CONVERSATION_UNLOCK";
    public static final String VMWARE_CONVERSATION_CONFIRM_MIGRATION = "VMWARE_CONVERSATION_CONFIRM_MIGRATION";
    public static final int VMWARE_TIMEOUT_FOR_LOAD_BALANCING = 200;

    ////// **** Constants for Allocator Agents  **** ///////
    public static final int PERIOD_FOR_PERIODICALLY_ATTEMPTING_TO_ALLOCATE_VM = 1000;


    ////// **** Constants for Host Agents  **** ///////
    public static final boolean ONLY_INTRA_LOAD_BALANCING = false;


    public static final int MIGRATION_CAUSE_HIGH_CPU = 0;
    public static final int MIGRATION_CAUSE_LOW_CPU = 1;
    public static final int MIGRATION_CAUSE_HIGH_MEMORY = 2;
    public static final int MIGRATION_CAUSE_LOW_MEMORY = 3;
    public static final int MIGRATION_CAUSE_VMWARE_JUST_CPU = 4;    // This is for VMWARE load balancing
    public static final int MIGRATION_CAUSE_VMWARE_JUST_MEMORY = 5; // This is for VMWARE load balancing


    public static final int TIMEOUT_FOR_CNP_INITIATOR_FOR_VM_MIGRATION = 150;
    public static final int TIMEOUT_FOR_CNP_INITIATOR_FOR_LOAD_BALANCING = 300;

    public static final int MAX_THRESHOLD_VIOLATION_COUNTER_FOR_HIGH_CPU = 10;
    public static final int MAX_THRESHOLD_VIOLATION_COUNTER_FOR_HIGH_MEMORY = 100000;
    public static final int MAX_THRESHOLD_VIOLATION_COUNTER_FOR_LOW_CPU = 10;
    public static final int MAX_THRESHOLD_VIOLATION_COUNTER_FOR_LOW_MEMORY = 100000;

    public static final boolean BALANCE_CPU = true;
    public static final boolean BALANCE_MEMORY = false;

    //from 16 cores to 224 cores
    // from 32 GiBs to 24 TiB 
    public static final int[][] HOST_OPTIONS = {{16, 32}, {64, 128}, {64, 256}, {64, 512}, {72, 192}, {72, 512}, {96, 192}, {96, 384}, {96, 768}, {448, 6144}, {448, 9216}, {448, 12288}, {448, 18432}, {448, 24576}};
    // https://aws.amazon.com/ec2/dedicated-hosts/pricing/#windows-dh
    
    /*
    test
    a1.metal 	16 	32
c6g.metal 	64 	128
m6g.metal 	64 	256
r6g.metal 	64 	512
c5n.metal 	72 	192
i3.metal 	72 	512
c5.metal 	96 	192
m5.metal 	96 	384
r5.metal 	96 	768
u-6tb1.metal 	448 	6144
u-9tb1.metal 	448 	9216
u-12tb1.metal 	448 	12288
u-18tb1.metal 	448 	18432
u-24tb1.metal 	448 	24576
    */

    // When comparing VMware against our work, DATACENTER_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE == HOST_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE
    public static final int TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE = 10;   // For a HOST_REPORTING_RATE of 1000 ms this means that the time window is 10 seconds
    // When comparing VMware against our work, DATACENTER_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE == HOST_TIME_WINDOW_IN_TERMS_OF_REPORTING_RATE
    public static final int DECISION_TYPE_DONT_MIGRATE = -1;
    public static final int DECISION_TYPE_MIGRATE_FROM_A_TO_B = 0;
    public static final int DECISION_TYPE_MIGRATE_FROM_B_TO_A = 1;


    ////// **** Constants for VM Agents  **** ///////

    // General Purpose Amazon EC2 M6g instances
    // Description available at https://aws.amazon.com/ec2/instance-types/
    // and https://aws.amazon.com/about-aws/whats-new/2020/05/amazon-ec2-m6g-instances-powered-by-aws-graviton2-processors-generally-available/
    public static final int[][] VM_OPTIONS = {{1, 4}, {2, 8}, {4, 16}, {8, 32}, {16, 64}, {32, 128}, {48, 192}, {64, 256}}; // {cores, GBs}
    public static final long VM_REPORTING_RATE = 1000; // in ms

    ////// **** Constants for Agent Conversations  **** ///////

    // conversation where host agents receive vm resource usage messages from virtual machine agents
    // conversation where virtual machine agents sends vm resource usage messages to host agents 
    public static final String CONVERSATION_MONITOR_VM = "CONVERSATION_MONITOR_VM";

    public static final String CONVERSATION_MIGRATE = "CONVERSATION_MIGRATE";

    // conversation where host agents receive registration messages from virtual machine agents
    // conversation where virtual machine agents sends registration messages to host agents 
    public static final String CONVERSATION_REGISTRATION_DUE_TO_VM_MIGRATION = "CONVERSATION_REGISTRATION_DUE_TO_VM_MIGRATION";

    // conversation where host agents execute contract net protocol for Load Balancing
    public static final String CONVERSATION_LOAD_BALANCING_A_TO_B = "CONVERSATION_LOAD_BALANCING_A_TO_B";
    public static final String CONVERSATION_LOAD_BALANCING_B_TO_A = "CONVERSATION_LOAD_BALANCING_B_TO_A";

    // conversation where host agents execute contract net protocol for Migrating VMs
    public static final String CONVERSATION_VM_MIGRATION = "CONVERSATION_VM_MIGRATION";

    // conversation where host agents execute contract net protocol for Hosting VMs
    public static final String CONVERSATION_VM_HOSTING = "CONVERSATION_VM_HOSTING";

    // conversation where allocator agents receive host resource usage messages from host agents
    public static final String CONVERSATION_MONITOR_HOST = "CONVERSATION_MONITOR_HOST";

    // conversation where workload generator agent sends vm requests to allocator agent
    // conversation where allocator agent receives vm requests from workload generator
    public static final String CONVERSATION_INITIAL_VM_REQUEST = "CONVERSATION_INITIAL_VM_REQUEST";

    // conversation where host agents notify virtual machine agents that they are hosted in their server
    // conversation where virtual machine agents receive notification of new owners
    // public static final String CONVERSATION_LISTEN_FOR_NEW_OWNER = "CONVERSATION_LISTEN_FOR_NEW_OWNER";
    // conversation where allocator agent allocates a VM to host agents
    public static final String CONVERSATION_VM_ALLOCATION = "CONVERSATION_VM_ALLOCATION";

    public static final String CONVERSATION_RESET_COUNTERS = "CONVERSATION_RESET_COUNTERS";
    public static final String CONVERSATION_A_COALITION_WAS_JUST_BALANCED = "CONVERSATION_A_COALITION_WAS_JUST_BALANCED";



    public static final String CONVERSATION_MODEL_UPDATE = "CONVERSATION_MODEL_UPDATE";
    public static final String CONVERSATION_FAILURE_NOTIFICATION = "CONVERSATION_FAILURE_NOTIFICATION";
    public static final String CONVERSATION_FAILURE_SUMMARY_FROM_LEADERS = "CONVERSATION_FAILURE_SUMMARY_FROM_LEADERS";



    ////// **** Constants for Emulation of VM resource usage **** ///////
    public static final int LOW_LONGTERM_MEAN_FOR_CPU_USAGE = 50;
    public static final int MEDIUM_LONGTERM_MEAN_FOR_CPU_USAGE = 75;
    public static final int HIGH_LONGTERM_MEAN_FOR_CPU_USAGE = 100;
    public static final int LOW_LONGTERM_MEAN_FOR_MEMORY_USAGE = 50;
    public static final int MEDIUM_LONGTERM_MEAN_FOR_MEMORY_USAGE = 75;
    public static final int HIGH_LONGTERM_MEAN_FOR_MEMORY_USAGE = 100;

    ////// **** Constants for debugging purposes and/or keeping a log **** ///////   
    public static final boolean EXCEPTIONS = true; // If enable prints exceptions
    public static final boolean LOG = true; // If (true) only prints experiment's data, otherwise it prints all the information for debugging purposes
    public static final boolean LOG_TO_FILE = false; // If (true) save data into output.txt else the system prints in console
    public static final boolean HOST_AGENT_GUI = true; // Enable or Disable HostAgents' GUIs
    public static final boolean ALLOCATOR_AGENT_GUI = false; // Enable or Disable AllocatorAgent's GUI
    public static final boolean WORKLOAD_GENERATOR_AGENT_GUI = false; // Enable or Disable AllocatorAgent's GUI

    ////// **** Constants for Failure Management **** ///////
    public static final boolean FAILURES_ARE_ENABLED = true;
    public static final String FAILURE_FROM_SWITCH = "FAILURE_FROM_SWITCH";
    public static final long TICKS_FOR_SWITCH_FAILURE_GENERATION = 1000; // ms
    public static final long TICKS_FOR_FAILURE_NOTIFICATION_TO_LEADERS = 1000;
    public static final double FAILURE_PROBABILITY = 0.01;
    public static final double MEAN_FAILURE_DURATION = 5;
    public static final double STD_DEV_FAILURE_DURATION = 2;


    ////// **** Constants for Failure Generation based on a Weibull Distribution **** ///////

    public static final double LEFT_BETA = 0.8;
    public static final double RIGHT_BETA = 5;

    // life span of two years = 120 + 245 + 365
    public static final int EARLY_LIFE_DAYS = 120;
    public static final int USEFUL_LIFE_DAYS = 245;
    public static final int WEAROUT_LIFE_DAYS = 365;
    public static final int LIFE_SPAN = EARLY_LIFE_DAYS + USEFUL_LIFE_DAYS + WEAROUT_LIFE_DAYS;

    public static final double MAX_EARLY_LIFE_FAILURE_RATE = 0.2;
    public static final double MAX_USEFUL_LIFE_FAILURE_RATE = 0.01;
    public static final double MAX_WEAROUT_LIFE_FAILURE_RATE = 0.4;
    public static final double MIN_FAILURE_RATE = 0.00000000001;

    private Consts() {
        throw new AssertionError();
    }
}
