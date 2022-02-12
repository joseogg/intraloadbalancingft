package intraloadbalancingft;

import java.util.stream.IntStream;

public class WeibullFailureGeneration {

    public double[] bathtub;

    public WeibullFailureGeneration() {
        this.bathtub = createBathtub();
    }

    private static double[] weibullHazard(double[] x, double beta) {
        double[] result = new double[x.length];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < x.length; i++) {
            result[i] = beta * (Math.pow(x[i], beta - 1));
            if (!Double.isFinite(result[i])) {
                result[i] = beta;
            }
            if (result[i] < min) {
                min = result[i];
            }
            if (result[i] > max) {
                max = result[i];
            }
        }
        if ((max - min) != 0) {
            for (int i = 0; i < result.length; i++) {
                result[i] = (result[i] - min) / (max - min);
            }
        }
        return result;
    }

    private static double[] createBathtub() {

        double[] xEarlyLife = IntStream.rangeClosed(0, Consts.EARLY_LIFE_DAYS - 1).mapToDouble(x -> x).toArray();
        double[] xUsefulLife = IntStream.rangeClosed(0, Consts.USEFUL_LIFE_DAYS - 1).mapToDouble(x -> x).toArray();
        double[] xWearoutLife = IntStream.rangeClosed(0, Consts.WEAROUT_LIFE_DAYS - 1).mapToDouble(x -> x).toArray();

        double[] leftPart = weibullHazard(xEarlyLife, Consts.LEFT_BETA);
        double[] centralPart = weibullHazard(xUsefulLife, 0);
        double[] rightPart = weibullHazard(xWearoutLife, Consts.RIGHT_BETA);

        double[] bathtub = new double[leftPart.length + centralPart.length + rightPart.length];
        for (int i = 0; i < bathtub.length; i++) {
            if (i < leftPart.length) {
                if (leftPart[i] < Consts.MIN_FAILURE_RATE) {
                    bathtub[i] = Consts.MIN_FAILURE_RATE;
                } else {
                    bathtub[i] = leftPart[i];
                }
            } else if (i < (leftPart.length + centralPart.length)) {
                if (centralPart[i - leftPart.length] < Consts.MIN_FAILURE_RATE) {
                    bathtub[i] = Consts.MIN_FAILURE_RATE;
                } else {
                    bathtub[i] = centralPart[i - leftPart.length];
                }
            } else {
                if (rightPart[i - leftPart.length - centralPart.length] < Consts.MIN_FAILURE_RATE) {
                    bathtub[i] = Consts.MIN_FAILURE_RATE;
                } else {
                    bathtub[i] = rightPart[i - leftPart.length - centralPart.length];
                }
            }
        }
        return bathtub;
    }

    public boolean failed(int day) {
        if (day < Consts.EARLY_LIFE_DAYS) {
            if (Math.random() < (Consts.MAX_EARLY_LIFE_FAILURE_RATE * bathtub[day])) {
                return true; // early life failure
            }
        } else if (day < Consts.USEFUL_LIFE_DAYS) {
            if (Math.random() < (Consts.MAX_USEFUL_LIFE_FAILURE_RATE * bathtub[day])) {
                return true; // useful life failure
            }
        } else {
            if (day >= bathtub.length) {
                day = bathtub.length - 1;
            }
            if (Math.random() < (Consts.MAX_WEAROUT_LIFE_FAILURE_RATE * bathtub[day])) {
                return true; // wearout life failure
            }
        }
        return false; // does not fail
    }

}
