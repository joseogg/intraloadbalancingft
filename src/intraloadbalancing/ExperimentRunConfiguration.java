package intraloadbalancing;

public class ExperimentRunConfiguration {
    private int TARGET_STD_DEV;
    private int NUMBER_OF_VMS;
    //        480	7
//        1620	6
//        3840	5
//        7500	4
//        12960	3
    private int INTRA_DISTRIBUTED_FIXED_COALITIONS = 0;
    private int VMWARE_CENTRALIZED_WITH_NO_COALITIONS = 1;
    private int LOAD_BALANCING_TYPE;

    private boolean BALANCING_ONLY_ONE_COALITION_AT_A_TIME;

    private int MIGRATION_THRESHOLD_FOR_LOW_CPU; // from 0 to 100 %
    private int MIGRATION_THRESHOLD_FOR_HIGH_CPU; // from 0 to 100 %
    private int MIGRATION_THRESHOLD_FOR_LOW_MEMORY; // from 0 to 100 %
    private int MIGRATION_THRESHOLD_FOR_HIGH_MEMORY; // from 0 to 100 %

    private String HEURISTIC;

    public int VMWARE_MAX_MIGRATIONS;

    public ExperimentRunConfiguration(int TARGET_STD_DEV, int NUMBER_OF_VMS, String BALANCING_TYPE, int ONLY_ONE_COALITION_AT_A_TIME, int MIGRATION_THRESHOLD_FOR_LOW_CPU, int MIGRATION_THRESHOLD_FOR_HIGH_CPU, int MIGRATION_THRESHOLD_FOR_LOW_MEMORY, int MIGRATION_THRESHOLD_FOR_HIGH_MEMORY, String HEURISTIC, int VMWARE_MAX_MIGRATIONS) {
        this.TARGET_STD_DEV = TARGET_STD_DEV;
        this.NUMBER_OF_VMS = NUMBER_OF_VMS;
        if (BALANCING_TYPE.equals("INTRA"))
            this.LOAD_BALANCING_TYPE = 0;
        else if (BALANCING_TYPE.equals("VMWARE"))
            this.LOAD_BALANCING_TYPE = 1;
        else
            this.LOAD_BALANCING_TYPE = -1;
        if (ONLY_ONE_COALITION_AT_A_TIME==0)
            this.BALANCING_ONLY_ONE_COALITION_AT_A_TIME = false;
        else
            this.BALANCING_ONLY_ONE_COALITION_AT_A_TIME = true;
        this.MIGRATION_THRESHOLD_FOR_LOW_CPU = MIGRATION_THRESHOLD_FOR_LOW_CPU;
        this.MIGRATION_THRESHOLD_FOR_HIGH_CPU = MIGRATION_THRESHOLD_FOR_HIGH_CPU;
        this.MIGRATION_THRESHOLD_FOR_LOW_MEMORY = MIGRATION_THRESHOLD_FOR_LOW_MEMORY;
        this.MIGRATION_THRESHOLD_FOR_HIGH_MEMORY = MIGRATION_THRESHOLD_FOR_HIGH_MEMORY;
        this.HEURISTIC= HEURISTIC;
        this.VMWARE_MAX_MIGRATIONS = VMWARE_MAX_MIGRATIONS;
    }

    public int getVMWARE_MAX_MIGRATIONS() {
        return VMWARE_MAX_MIGRATIONS;
    }

    public String getHEURISTIC() {
        return HEURISTIC;
    }

    public int getTARGET_STD_DEV() {
        return TARGET_STD_DEV;
    }

    public int getNUMBER_OF_VMS() {
        return NUMBER_OF_VMS;
    }

    public int getINTRA_DISTRIBUTED_FIXED_COALITIONS() {
        return INTRA_DISTRIBUTED_FIXED_COALITIONS;
    }

    public int getVMWARE_CENTRALIZED_WITH_NO_COALITIONS() {
        return VMWARE_CENTRALIZED_WITH_NO_COALITIONS;
    }

    public int getLOAD_BALANCING_TYPE() {
        return LOAD_BALANCING_TYPE;
    }

    public boolean isBALANCING_ONLY_ONE_COALITION_AT_A_TIME() {
        return BALANCING_ONLY_ONE_COALITION_AT_A_TIME;
    }

    public int getMIGRATION_THRESHOLD_FOR_LOW_CPU() {
        return MIGRATION_THRESHOLD_FOR_LOW_CPU;
    }

    public int getMIGRATION_THRESHOLD_FOR_HIGH_CPU() {
        return MIGRATION_THRESHOLD_FOR_HIGH_CPU;
    }

    public int getMIGRATION_THRESHOLD_FOR_LOW_MEMORY() {
        return MIGRATION_THRESHOLD_FOR_LOW_MEMORY;
    }

    public int getMIGRATION_THRESHOLD_FOR_HIGH_MEMORY() {
        return MIGRATION_THRESHOLD_FOR_HIGH_MEMORY;
    }
}
