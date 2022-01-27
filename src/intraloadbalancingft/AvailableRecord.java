package intraloadbalancingft;

public class AvailableRecord {
    private long currentAvailability;

    public AvailableRecord(long xCurAvailableRecord){
        this.currentAvailability=xCurAvailableRecord;
    }

    @Override
    public String toString() {
        return "AvailableRecord{" +
                "currentAvailability=" + currentAvailability+
                '}';
    }

    public long getCurrentAvailability(){
        return this.currentAvailability;
    }

    public void setCurrentAvailability(long xNew){
        this.currentAvailability=xNew;
    }
}
