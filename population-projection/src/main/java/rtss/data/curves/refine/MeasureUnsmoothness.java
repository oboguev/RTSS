package rtss.data.curves.refine;

import rtss.data.curves.refine.RefineYearlyPopulationCore.Objective;
import rtss.math.algorithms.Entropy;
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
         * Однако это вело к спрямлению кривой в прямую, т.к. фукнционал пытался выравнять значения d1 на промежутке.
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
        ov.smoothnessGini = Util.gini(rebasePositive(d2));

        // ov.smoothnessVariancePenalty = Util.averageDeviation(ad3);
        // double smoothnessViolation = ov.smoothnessMagnitutePenalty + SmoothnessVariancePenaltyWeight * ov.smoothnessVariancePenalty;

        double smoothnessViolation = ov.smoothnessMagnitutePenalty + ov.smoothnessGini + Math.sqrt(ov.smoothnessMagnitutePenalty * ov.smoothnessGini);
        
        // ### penalize uneven distribution of d2
        double z = Entropy.concentration(rebasePositive(d2));
    
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
    
    private double[] rebasePositive(double[] p)
    {
        double v = Util.min(p);
        if (v >= 0)
            return p;
        else
            return Util.add(p, -v);
    }
}
