package intraloadbalancingft;

// Weibull distribution
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.distribution.WeibullDistribution;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class test {


    private static RandomGenerator rg = new JDKRandomGenerator();
    private static WeibullDistribution  weibullDistribution = new WeibullDistribution(rg, 10000, 1, WeibullDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);




    public static void main(String[] args) throws Exception {
        ArrayList data =  new ArrayList();
        for (int i = 0; i < 1000; i++)
            data.add(weibullDistribution.sample());

        Iterator e = data.iterator();
        System.out.println("[");
        while (e.hasNext()){
            System.out.println(e.next()+",");
        }
        System.out.println("]");


/*
        double x[] = {1500, 1870, 2010, 2010, 2720, 2900, 3020, 3060, 3060, 3180, 3200, 3210, 3210, 3260,
                3300, 3300, 3300, 3420, 3460, 3480, 3580, 3610, 3620, 3700, 3790, 3810, 3900, 3920,
                3940, 3970, 4000, 4000, 4100, 4130, 4130, 4210, 4230, 4260, 4300, 4300, 4350, 4370,
                4380, 4420, 4470, 4470, 4490, 4490, 4570, 4600, 4710, 4730, 4820, 4850, 4910, 4930,
                4990, 4990, 5100, 5210, 5350, 5400, 5670, 5790, 5840, 5900, 5950, 5970, 7800};
        double A[] = {26, 31, 35, 38, 41, 44, 47, 51, 55, 70};
        double B[] = {16, 24, 30, 36, 42, 48, 55, 63, 76, 126, 52};

        double[] parameters = WeibullDist.getMLE(x, x.length);
        WeibullDist w = new WeibullDist(parameters[0],parameters[1],parameters[2]);
        System.out.println("(SSJ - MLE) Alpha (shape) = " + w.getAlpha());
        System.out.println("(SSJ - MLE) Lambda = " + w.getLambda());
        System.out.println("(SSJ - MLE) Delta = " + w.getDelta());
        System.out.println("(MTBF) Mean= "+w.getMean());
        System.out.println("Variance = "+w.getVariance());
        System.out.println("SD = "+w.getStandardDeviation());

        // We use the method solve() of the class NewtonRaphsonSolver of Apache Commons Math
        // to compute the estimation of the shape parameter  (beta), which use Newton-Rhapson approach,
        // whereas for the scale (eta) we use the evaluate() method of the Sum class
        // after we have the result for shape.
        ProbabilityPlot pp = new ProbabilityPlot(x);
        pp.suppressDisplay();
        System.out.println("(flanagan - MLE) Beta (Shape)= " + pp.weibullTwoParGamma());
        System.out.println("(flanagan - MLE) Eta (Scale)= " + pp.weibullTwoParSigma());
        System.out.println("(flanagan - MLE) Intercept = " + pp.weibullTwoParIntercept());
        System.out.println("(flanagan - MLE) Delta = " + pp.delta());
        System.out.println("(MTBF) = " + pp.weibullMu());

        //Regression reg = new Regression(pp.weibullTwoParOrderStatisticMedians(), x);
        //UnivariateDifferentiableFunction f = (UnivariateDifferentiableFunction) new WeibullDistribution(parameters[0], parameters[1]);
        //NewtonRaphsonSolver nrs = new NewtonRaphsonSolver();
        //WeibullDistribution w2 = new WeibullDistribution(par[0], par[1], WeibullDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        //System.out.println(w2.getShape());
        //System.out.println(w2.getScale());
        //System.out.println(w2.getNumericalVariance());


        CSVLoader csv = new CSVLoader();
        String filename = "./Default.csv";
        csv.setSource(new File(filename));
        Instances data = csv.getDataSet();


        int lastIndex = data.numAttributes() - 1;
        data.setClassIndex(lastIndex);

        // crea el modelo
        Logistic lg_model = new Logistic();
        // lo entrena
        try {
            lg_model.buildClassifier(data);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        // crea una instance desde cero para probar
        Instance inst = new DenseInstance(4);
        inst.setValue(0, 1); // Beta 0
        inst.setValue(1, 0); // Beta 1 * X1
        inst.setValue(2, 1500); // Beta 2 * X2
        inst.setValue(3, 40000); // Beta 3 * X3
        inst.setDataset(data);
        double index = lg_model.classifyInstance(inst);
        System.out.println(index);
        System.out.println("Coeficientes: ");
        double[][] coefficients = lg_model.coefficients();
        double linear_expression = 0;
        for(int i=0;i<coefficients.length;i++) {
            System.out.println(coefficients[i][0]);
            linear_expression += coefficients[i][0] * inst.value(i);
        }
        double probability = Math.exp(linear_expression) / (1 + Math.exp(linear_expression));
        System.out.println("Probabilidad de no fallar = " + probability);
        System.out.println("Probabilidad de fallar = " + (1-probability));
        String className = data.attribute(lastIndex).value((int)index);
        System.out.println("Predicción = " + className);
*/





    }

}
