package intraloadbalancingft;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class DataCenterInformation implements Serializable {

    private Map<String, ArrayList<HostDescription>> dataCenterHostsInformation; // coalition id, list of hosts' descriptions
    private Map<String, Map<String, ArrayList<FailureRecord>>> dataCenterFailures;

    public DataCenterInformation(Map<String, ArrayList<HostDescription>> dataCenterHostsInformation, Map<String, Map<String, ArrayList<FailureRecord>>> dataCenterFailures) {
        this.dataCenterHostsInformation = dataCenterHostsInformation;
        this.dataCenterFailures = dataCenterFailures;
    }


}
