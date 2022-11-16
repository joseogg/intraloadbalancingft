package intraloadbalancingft;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DataCenterInformation implements Serializable {


    private Map<String, ArrayList<ShallowHostDescription>> dataCenterHostsInformation; // coalition id, list of hosts' descriptions
    private Map<String, Map<String, ArrayList<FailureRecord>>> dataCenterFailures;
    private long timeWindow;
    private long currentTime;

    public DataCenterInformation(Map<String, ArrayList<HostDescription>> dataCenterHostsInformation, Map<String, Map<String, ArrayList<FailureRecord>>> dataCenterFailures, long timeWindow, long currentTime) {

        this.dataCenterHostsInformation = new HashMap<String, ArrayList<ShallowHostDescription>>();
        if (dataCenterHostsInformation != null) {

            for (Map.Entry<String, ArrayList<HostDescription>> entry : dataCenterHostsInformation.entrySet()){
//                System.out.println("-------------------Key = " + entry.getKey() + ", Value = " + entry.getValue());
                ArrayList<ShallowHostDescription> listShallowHostDescription = new ArrayList<ShallowHostDescription>();
                for (int i = 0; i < entry.getValue().size(); i++){
                    ShallowHostDescription shallowHostDescription = new ShallowHostDescription(entry.getValue().get(i));
                    listShallowHostDescription.add(shallowHostDescription);
                }
                this.dataCenterHostsInformation.put(entry.getKey(), listShallowHostDescription);
            }

        }

        this.dataCenterFailures = dataCenterFailures;
        this.timeWindow = timeWindow;
        this.currentTime = currentTime;
        //
    }
/*

{"dataCenterHostsInformation":{
    "HostAgent20":[
        {"id":"HostAgent22","coalition":20,"memoryUsage":11.984492890104164,"CPUUsage":73.77305325779098,"memory":12288.0,"memoryUsed":1792.0,"numberOfVirtualCores":448,"numberOfVirtualCoresUsed":448,"failed":false,
            "virtualMachinesHosted":[
                {"id":"VirtualMachineAgent11","numberOfVirtualCores":4,"memory":16,"CPUUsage":33.897433223967525,"memoryUsage":96.53028251410232},
                {"id":"VirtualMachineAgent13","numberOfVirtualCores":64,"memory":256,"CPUUsage":73.9006611731014,"memoryUsage":98.72163691362239},
                {"id":"VirtualMachineAgent18","numberOfVirtualCores":2,"memory":8,"CPUUsage":82.48243451312878,"memoryUsage":60.98600818975721},
                {"id":"VirtualMachineAgent25","numberOfVirtualCores":32,"memory":128,"CPUUsage":75.6904590749226,"memoryUsage":97.8502697582436},
                {"id":"VirtualMachineAgent29","numberOfVirtualCores":2,"memory":8,"CPUUsage":51.114804768837416,"memoryUsage":56.570818179169365},
                {"id":"VirtualMachineAgent34","numberOfVirtualCores":4,"memory":16,"CPUUsage":98.46023058268626,"memoryUsage":57.062146712030476},
                {"id":"VirtualMachineAgent38","numberOfVirtualCores":32,"memory":128,"CPUUsage":47.650304401039996,"memoryUsage":54.828061745630414},
                {"id":"VirtualMachineAgent41","numberOfVirtualCores":1,"memory":4,"CPUUsage":35.486222447681584,"memoryUsage":86.26772275060543},
                {"id":"VirtualMachineAgent46","numberOfVirtualCores":8,"memory":32,"CPUUsage":91.02240290611607,"memoryUsage":89.5118587392264},
                {"id":"VirtualMachineAgent47","numberOfVirtualCores":2,"memory":8,"CPUUsage":95.07081418999942,"memoryUsage":93.17162933217047},
                {"id":"VirtualMachineAgent51","numberOfVirtualCores":1,"memory":4,"CPUUsage":82.20670159203112,"memoryUsage":89.1039524284063},
                {"id":"VirtualMachineAgent52","numberOfVirtualCores":16,"memory":64,"CPUUsage":81.98788059374205,"memoryUsage":100.0},
                {"id":"VirtualMachineAgent53","numberOfVirtualCores":64,"memory":256,"CPUUsage":91.27825464108284,"memoryUsage":87.7514566756358},
                {"id":"VirtualMachineAgent43","numberOfVirtualCores":8,"memory":32,"CPUUsage":81.72298311083784,"memoryUsage":55.365968510305734},
                {"id":"VirtualMachineAgent45","numberOfVirtualCores":8,"memory":32,"CPUUsage":67.53590087817615,"memoryUsage":68.79248366132492},
                {"id":"VirtualMachineAgent59","numberOfVirtualCores":32,"memory":128,"CPUUsage":70.61650533980158,"memoryUsage":62.66495585628472},
                {"id":"VirtualMachineAgent60","numberOfVirtualCores":2,"memory":8,"CPUUsage":54.19847391578698,"memoryUsage":97.29160529179427},
                {"id":"VirtualMachineAgent67","numberOfVirtualCores":64,"memory":256,"CPUUsage":58.190166809555265,"memoryUsage":87.54804166816459},
                {"id":"VirtualMachineAgent70","numberOfVirtualCores":1,"memory":4,"CPUUsage":52.387039453518454,"memoryUsage":87.27890587320374},
                {"id":"VirtualMachineAgent36","numberOfVirtualCores":64,"memory":256,"CPUUsage":97.58291241604353,"memoryUsage":65.72704076944773},
                {"id":"VirtualMachineAgent37","numberOfVirtualCores":2,"memory":8,"CPUUsage":50.47750359967187,"memoryUsage":63.8904086807165},
                {"id":"VirtualMachineAgent55","numberOfVirtualCores":2,"memory":8,"CPUUsage":83.96988032689555,"memoryUsage":81.74011785146949},
                {"id":"VirtualMachineAgent61","numberOfVirtualCores":16,"memory":64,"CPUUsage":25.024017753589785,"memoryUsage":89.98649917112705},
                {"id":"VirtualMachineAgent84","numberOfVirtualCores":4,"memory":16,"CPUUsage":52.49803749741718,"memoryUsage":89.09745853728545},
                {"id":"VirtualMachineAgent85","numberOfVirtualCores":4,"memory":16,"CPUUsage":81.7398626066687,"memoryUsage":99.79225933112897},
                {"id":"VirtualMachineAgent91","numberOfVirtualCores":8,"memory":32,"CPUUsage":66.3774485288902,"memoryUsage":99.24023299798735},
                {"id":"VirtualMachineAgent120","numberOfVirtualCores":1,"memory":4,"CPUUsage":66.21727612548968,"memoryUsage":98.54501466584048}]},
        {"id":"HostAgent20","coalition":20,"memoryUsage":74.46021842162298,"CPUUsage":69.3556043697315,"memory":384.0,"memoryUsed":384.0,"numberOfVirtualCores":96,"numberOfVirtualCoresUsed":96,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent1","numberOfVirtualCores":48,"memory":192,"CPUUsage":89.89908805169148,"memoryUsage":87.65507726231571},{"id":"VirtualMachineAgent5","numberOfVirtualCores":48,"memory":192,"CPUUsage":48.81212068777149,"memoryUsage":61.26535958093026}]},
        {"id":"HostAgent23","coalition":20,"memoryUsage":0.0,"CPUUsage":0.0,"memory":512.0,"memoryUsed":288.0,"numberOfVirtualCores":72,"numberOfVirtualCoresUsed":72,"failed":true,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent15","numberOfVirtualCores":64,"memory":256,"CPUUsage":49.9606343663928,"memoryUsage":37.73028825021597},{"id":"VirtualMachineAgent21","numberOfVirtualCores":2,"memory":8,"CPUUsage":98.47846972027715,"memoryUsage":62.0722127594827},{"id":"VirtualMachineAgent30","numberOfVirtualCores":4,"memory":16,"CPUUsage":60.41735048895226,"memoryUsage":99.36549865447473},{"id":"VirtualMachineAgent57","numberOfVirtualCores":1,"memory":4,"CPUUsage":96.57801980375224,"memoryUsage":98.14526717331435},{"id":"VirtualMachineAgent122","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0}]},
        {"id":"HostAgent21","coalition":20,"memoryUsage":20.80472094053707,"CPUUsage":76.28658821656761,"memory":6144.0,"memoryUsed":1792.0,"numberOfVirtualCores":448,"numberOfVirtualCoresUsed":448,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent4","numberOfVirtualCores":2,"memory":8,"CPUUsage":86.3792715225015,"memoryUsage":86.42473401032873},{"id":"VirtualMachineAgent9","numberOfVirtualCores":64,"memory":256,"CPUUsage":64.68300484510073,"memoryUsage":58.22372920832508},{"id":"VirtualMachineAgent10","numberOfVirtualCores":8,"memory":32,"CPUUsage":64.5165577484756,"memoryUsage":65.94020036403013},{"id":"VirtualMachineAgent14","numberOfVirtualCores":1,"memory":4,"CPUUsage":68.78106634945424,"memoryUsage":93.47285371656156},{"id":"VirtualMachineAgent19","numberOfVirtualCores":16,"memory":64,"CPUUsage":47.3576453153958,"memoryUsage":55.698212683887334},{"id":"VirtualMachineAgent20","numberOfVirtualCores":16,"memory":64,"CPUUsage":77.91226391916635,"memoryUsage":83.37252365231829},{"id":"VirtualMachineAgent32","numberOfVirtualCores":8,"memory":32,"CPUUsage":40.18565463538367,"memoryUsage":93.81564377158915},{"id":"VirtualMachineAgent49","numberOfVirtualCores":64,"memory":256,"CPUUsage":71.85672274454781,"memoryUsage":76.64109433774325},{"id":"VirtualMachineAgent33","numberOfVirtualCores":48,"memory":192,"CPUUsage":58.44704458438744,"memoryUsage":48.832331502982974},{"id":"VirtualMachineAgent56","numberOfVirtualCores":4,"memory":16,"CPUUsage":84.03609579787049,"memoryUsage":96.73118056024241},{"id":"VirtualMachineAgent68","numberOfVirtualCores":8,"memory":32,"CPUUsage":66.72359183712717,"memoryUsage":92.29422001372771},{"id":"VirtualMachineAgent69","numberOfVirtualCores":4,"memory":16,"CPUUsage":49.97073216304363,"memoryUsage":97.85482297636247},{"id":"VirtualMachineAgent72","numberOfVirtualCores":1,"memory":4,"CPUUsage":84.20747947826325,"memoryUsage":58.52869185443593},{"id":"VirtualMachineAgent73","numberOfVirtualCores":4,"memory":16,"CPUUsage":40.12387368758774,"memoryUsage":85.53630672481981},{"id":"VirtualMachineAgent75","numberOfVirtualCores":64,"memory":256,"CPUUsage":100.0,"memoryUsage":100.0},{"id":"VirtualMachineAgent62","numberOfVirtualCores":48,"memory":192,"CPUUsage":100.0,"memoryUsage":78.25571219277465},{"id":"VirtualMachineAgent87","numberOfVirtualCores":32,"memory":128,"CPUUsage":82.38128398260909,"memoryUsage":48.09722120769647},{"id":"VirtualMachineAgent90","numberOfVirtualCores":4,"memory":16,"CPUUsage":48.12597375932703,"memoryUsage":74.31512585630605},{"id":"VirtualMachineAgent54","numberOfVirtualCores":16,"memory":64,"CPUUsage":71.88797388763295,"memoryUsage":58.99756719701301},{"id":"VirtualMachineAgent79","numberOfVirtualCores":2,"memory":8,"CPUUsage":59.810457169202124,"memoryUsage":72.78121258307404},{"id":"VirtualMachineAgent94","numberOfVirtualCores":16,"memory":64,"CPUUsage":93.46000181911705,"memoryUsage":33.29848378906754},{"id":"VirtualMachineAgent80","numberOfVirtualCores":8,"memory":32,"CPUUsage":84.54803390548904,"memoryUsage":99.38542206388608},{"id":"VirtualMachineAgent112","numberOfVirtualCores":8,"memory":32,"CPUUsage":77.8726690622803,"memoryUsage":100.0},{"id":"VirtualMachineAgent127","numberOfVirtualCores":1,"memory":4,"CPUUsage":65.0157762248076,"memoryUsage":49.60328332545823},{"id":"VirtualMachineAgent159","numberOfVirtualCores":1,"memory":4,"CPUUsage":76.1210301524111,"memoryUsage":34.246528257281824}]}],

    "HostAgent30":[
        {"id":"HostAgent32","coalition":30,"memoryUsage":0.12379624993670103,"CPUUsage":2.203646814617726,"memory":24576.0,"memoryUsed":1792.0,"numberOfVirtualCores":448,"numberOfVirtualCoresUsed":448,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent23","numberOfVirtualCores":8,"memory":32,"CPUUsage":91.25193097244512,"memoryUsage":65.8742818345469},{"id":"VirtualMachineAgent27","numberOfVirtualCores":4,"memory":16,"CPUUsage":64.30458129229507,"memoryUsage":58.40247623367898},{"id":"VirtualMachineAgent30","numberOfVirtualCores":4,"memory":16,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent33","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent35","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent36","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent37","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent45","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent55","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent50","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent61","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent64","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent66","numberOfVirtualCores":4,"memory":16,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent71","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent54","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent74","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent78","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent50","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent64","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent65","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent76","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent76","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent110","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0}]},
        {"id":"HostAgent30","coalition":30,"memoryUsage":63.63883219539556,"CPUUsage":46.405205616259664,"memory":192.0,"memoryUsed":192.0,"numberOfVirtualCores":72,"numberOfVirtualCoresUsed":48,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent2","numberOfVirtualCores":16,"memory":64,"CPUUsage":38.341751436086334,"memoryUsage":96.43920861705466},{"id":"VirtualMachineAgent6","numberOfVirtualCores":8,"memory":32,"CPUUsage":49.5210914207918,"memoryUsage":89.61691396048504},{"id":"VirtualMachineAgent8","numberOfVirtualCores":16,"memory":64,"CPUUsage":97.19056685359449,"memoryUsage":31.797594789566606},{"id":"VirtualMachineAgent39","numberOfVirtualCores":8,"memory":32,"CPUUsage":97.06112254618354,"memoryUsage":35.74247239864574}]},
        {"id":"HostAgent31","coalition":30,"memoryUsage":5.482931958481386,"CPUUsage":12.940169960527893,"memory":9216.0,"memoryUsed":1792.0,"numberOfVirtualCores":448,"numberOfVirtualCoresUsed":448,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent7","numberOfVirtualCores":2,"memory":8,"CPUUsage":100.0,"memoryUsage":50.728409627750196},{"id":"VirtualMachineAgent12","numberOfVirtualCores":64,"memory":256,"CPUUsage":42.08638304980251,"memoryUsage":75.31286316485685},{"id":"VirtualMachineAgent22","numberOfVirtualCores":64,"memory":256,"CPUUsage":31.652033082339017,"memoryUsage":95.12095625236856},{"id":"VirtualMachineAgent24","numberOfVirtualCores":1,"memory":4,"CPUUsage":61.06053294457263,"memoryUsage":100.0},{"id":"VirtualMachineAgent26","numberOfVirtualCores":32,"memory":128,"CPUUsage":25.58959139011092,"memoryUsage":45.03594751493614},{"id":"VirtualMachineAgent43","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent50","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent36","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent37","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent54","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent77","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent63","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent83","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent92","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent77","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent101","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent128","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0}]},
        {"id":"HostAgent33","coalition":30,"memoryUsage":2.0053157434404527,"CPUUsage":34.70538798945011,"memory":24576.0,"memoryUsed":1792.0,"numberOfVirtualCores":448,"numberOfVirtualCoresUsed":448,"failed":false,
            "virtualMachinesHosted":[{"id":"VirtualMachineAgent3","numberOfVirtualCores":8,"memory":32,"CPUUsage":93.55869722281948,"memoryUsage":92.51944779512698},{"id":"VirtualMachineAgent17","numberOfVirtualCores":48,"memory":192,"CPUUsage":80.40157681596025,"memoryUsage":40.35837402395568},{"id":"VirtualMachineAgent31","numberOfVirtualCores":64,"memory":256,"CPUUsage":80.49535308175284,"memoryUsage":92.80243307783714},{"id":"VirtualMachineAgent40","numberOfVirtualCores":2,"memory":8,"CPUUsage":70.56092688407254,"memoryUsage":48.65439225830067},{"id":"VirtualMachineAgent42","numberOfVirtualCores":1,"memory":4,"CPUUsage":100.0,"memoryUsage":64.87522631817116},{"id":"VirtualMachineAgent44","numberOfVirtualCores":48,"memory":192,"CPUUsage":71.3483071447506,"memoryUsage":24.554730271140887},{"id":"VirtualMachineAgent48","numberOfVirtualCores":1,"memory":4,"CPUUsage":85.48201852200764,"memoryUsage":50.79572679684697},{"id":"VirtualMachineAgent35","numberOfVirtualCores":8,"memory":32,"CPUUsage":80.71211540637965,"memoryUsage":96.21994911605907},{"id":"VirtualMachineAgent58","numberOfVirtualCores":16,"memory":64,"CPUUsage":86.97165116272505,"memoryUsage":96.41126822692631},{"id":"VirtualMachineAgent62","numberOfVirtualCores":48,"memory":192,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent65","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent76","numberOfVirtualCores":2,"memory":8,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent80","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent81","numberOfVirtualCores":16,"memory":64,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent82","numberOfVirtualCores":64,"memory":256,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent66","numberOfVirtualCores":4,"memory":16,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent86","numberOfVirtualCores":32,"memory":128,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent78","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent80","numberOfVirtualCores":8,"memory":32,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent96","numberOfVirtualCores":4,"memory":16,"CPUUsage":0.0,"memoryUsage":0.0},{"id":"VirtualMachineAgent100","numberOfVirtualCores":1,"memory":4,"CPUUsage":0.0,"memoryUsage":0.0}]}]},"dataCenterFailures":{"HostAgent20":{"HostAgent20":[],"HostAgent23":[{"startFailure":1663249600961,"endFailure":1663249603965}],"HostAgent22":[{"startFailure":1663249605032,"endFailure":1663249609038}]},"HostAgent30":{"HostAgent31":[{"startFailure":1663249605962,"endFailure":1663249610969}],"HostAgent30":[{"startFailure":1663249597005,"endFailure":1663249604027}],"HostAgent33":[{"startFailure":1663249597985,"endFailure":1663249603992}],"HostAgent32":[]}},

    "timeWindow":10000,

    "currentTime":1663249612934}

 */

    private class ShallowHostDescription implements java.io.Serializable {

        private String id = "";
        private int coalition = -1;
        private double memoryUsage = 0;
        private double CPUUsage = 0;
        private double memory = 0;
        private double memoryUsed = 0;
        private int numberOfVirtualCores = 0;
        private int numberOfVirtualCoresUsed = 0;
        private boolean failed = false;
        private ArrayList<ShallowVirtualMachineDescription> virtualMachinesHosted;

        public ShallowHostDescription(HostDescription hostDescription) {
            this.id = hostDescription.getId();
            this.coalition = hostDescription.getCoalition();
            this.memoryUsage = hostDescription.getMemoryUsage();
            this.CPUUsage = hostDescription.getCPUUsage();
            this.memory = hostDescription.getMemory();
            this.memoryUsed =  hostDescription.getMemoryUsed();
            this.numberOfVirtualCores = hostDescription.getNumberOfVirtualCores();
            this.numberOfVirtualCoresUsed = hostDescription.getNumberOfVirtualCoresUsed();
            this.failed = hostDescription.isFailed();
            this.virtualMachinesHosted = new ArrayList<ShallowVirtualMachineDescription>();
            for (int i=0; i<hostDescription.getVirtualMachinesHosted().size(); i++){
                ShallowVirtualMachineDescription shallowVirtualMachineDescription = new ShallowVirtualMachineDescription(hostDescription.getVirtualMachinesHosted().get(i));
                this.virtualMachinesHosted.add(shallowVirtualMachineDescription);
            }
        }
    }

    private class ShallowVirtualMachineDescription implements java.io.Serializable {
        private String id;
        private int numberOfVirtualCores;
        private int memory;                 // in GBs
        private double CPUUsage;            // value from 0 to 100
        private double memoryUsage;         // value from 0 to 100

        public ShallowVirtualMachineDescription(VirtualMachineDescription virtualMachineDescription) {
            this.id = virtualMachineDescription.getId();
            this.numberOfVirtualCores = virtualMachineDescription.getNumberOfVirtualCores();
            this.memory = virtualMachineDescription.getMemory();
            this.CPUUsage = virtualMachineDescription.getCPUUsage();
            this.memoryUsage = virtualMachineDescription.getMemoryUsage();
        }
    }

}
