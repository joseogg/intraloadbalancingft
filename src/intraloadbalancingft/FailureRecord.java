package intraloadbalancingft;

import java.io.Serializable;

public class FailureRecord implements Serializable {
    private long startFailure;
    private long endFailure;


    public FailureRecord(long startFailure, long endFailure) {
        this.startFailure = startFailure;
        this.endFailure = endFailure;
    }

    @Override
    public String toString() {
        return "FailureRecord{" +
                "startFailure=" + startFailure +
                ", endFailure=" + endFailure +
                '}';
    }

    public long getFailureTime() {
        return endFailure - startFailure;
    }

    public long getStartFailure() {
        return startFailure;
    }

    public void setStartFailure(long startFailure) {
        this.startFailure = startFailure;
    }

    public long getEndFailure() {
        return endFailure;
    }

    public void setEndFailure(long endFailure) {
        this.endFailure = endFailure;
    }
}

