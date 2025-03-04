package rtss.data.curves.refine;

import java.util.Arrays;

import rtss.data.curves.refine.RefineYearlyPopulationCore.Objective;
// import rtss.math.algorithms.Entropy;
import rtss.util.Util;

/*
 * Measure a smoothness of a curve segment
 */
class MeasureUnsmoothness
{
    double calculateSmoothnessViolation(double[] p, Objective ov)
    {
        /*
         * Первоначальная версия функционала пыталась минимизировать sum(abs(d2)).
         * Однако это вело к спрямлению кривой в прямую, т.к. функнционал пытался выравнять значения d1 на промежутке.
         * 
         * Цель же состоит в том, чтобы штрафовать только резкие изломы линии, позволяя однако гладкие вогнутые или выпуклые линии (похожие на параболу),
         * обладающие ненулевой (но невысокой) второй производной.
         * 
         * Третья производная измеряет изменение второй производной. Если вторая производная резко меняется (указывая на зубчатый край), 
         * третья производная будет большой. Налагая штраф на большие абсолютные значения третьей производной, мы препятствуем зубчатым краям, 
         * допуская при этом плавные кривые.
         */
        p = Util.normalize(p);
        double[] d2 = d2(p);
        double[] d3 = derivative(d2);
        double[] ad3 = Util.abs(d3);

        ov.smoothnessMagnitutePenalty = Util.sum(ad3);

        /*
         * penalize uneven distribution of d2
         */
        // ov.smoothnessVariancePenalty = Util.averageDeviation(ad3);
        // ov.smoothnessGini = 0;
        // ov.smoothnessGini = differentness_1(d2);
        // ov.smoothnessGini = Entropy.concentration(rebasePositive(d2));
        ov.smoothnessGini = 50_000 * differentness_2(d2);

        double smoothnessViolation = ov.smoothnessMagnitutePenalty + ov.smoothnessGini + Math.sqrt(ov.smoothnessMagnitutePenalty * ov.smoothnessGini);

        return smoothnessViolation;
    }

    private double[] derivative(double[] p)
    {
        if (p.length <= 1)
            return new double[0];

        double[] d = new double[p.length - 1];
        for (int i = 0; i <= p.length - 2; i++)
            d[i] = p[i + 1] - p[i];
        return d;
    }

    private double[] d2(double[] p)
    {
        return derivative(derivative(p));
    }

    @SuppressWarnings("unused")
    private double[] d3(double[] p)
    {
        return derivative(d2(p));
    }

    @SuppressWarnings("unused")
    private double[] rebasePositive(double[] p)
    {
        double v = Util.min(p);
        if (v >= 0)
            return p;
        else
            return Util.add(p, -v);
    }

    /* 
     * Normalized dispersion measure that accounts for relative differences between elements. 
     * 
     * Absolute Pairwise Differences: Compute all pairwise absolute differences in the dataset.
     * Normalize Differences: Normalize by the total sum of absolute values to keep the result between 0 and 1.
     * Adjust for Negative Values: Since values can be negative, we shift the data so the dispersion metric remains meaningful.
     * 
     * Near 0 when values are similar.
     * Close to 1 when there are large outliers.
    */
    @SuppressWarnings("unused")
    private double differentness_1(double[] p)
    {
        int n = p.length;
        if (n == 0)
            return 0; // Edge case: empty array

        // Compute sum of absolute values
        double sumAbs = Util.sum(Util.abs(p));
        if (sumAbs == 0)
            return 0; // Edge case: all zeros

        // Compute sum of absolute pairwise differences
        double sumDiffs = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                sumDiffs += Math.abs(p[i] - p[j]);
            }
        }

        // Compute and return differentness metric
        return (sumDiffs / (2.0 * n * sumAbs));
    }

    /*
     * Compute variance of second derivative
     */
    private double differentness_2(double[] d2)
    {
        if (d2.length < 1)
            throw new IllegalArgumentException("Series must have at least 3 points");

        // Compute mean of second derivative
        double mean = Arrays.stream(d2).average().getAsDouble();

        // Compute variance of second derivative
        double variance = Arrays.stream(d2)
                .map(v -> (v - mean) * (v - mean))
                .average()
                .getAsDouble();

        return variance;
    }
}
