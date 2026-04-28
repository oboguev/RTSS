package rtss.data.mortality;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Gender;
import rtss.util.Util;

public class MortalityUtil
{
    private static final int NPOINTS_QX2MX = 1000;
    private static final int MX2QX_TABLE_SIZE = 1000;
    private static double[] mx2qx_table_mx = null;
    private static double[] mx2qx_table_qx = null;
    private static boolean useMxTable = true;

    public static final double MAX_MX = 2.0;

    public static double qx2mx(double qx, Gender gender, double age) throws Exception
    {
        if (age >= 1.0)
            return qx2mx(qx);
        else
            return q0ToM0(qx, gender);
    }

    public static double mx2qx(double mx, Gender gender, double age) throws Exception
    {
        if (age >= 1.0)
            return mx2qx(mx);
        else
            return m0ToQ0(mx, gender);
    }

    private static double q0ToM0(double q0, Gender gender)
    {
        q0 = validate_qx(q0);

        if (q0 == 1.0)
            return Double.POSITIVE_INFINITY;

        double a0 = a0FromQ0CoaleDemeny(q0, gender);

        return q0 / (1.0 - (1.0 - a0) * q0);
    }

    private static double m0ToQ0(double m0, Gender gender)
    {
        m0 = validate_mx(m0);

        if (m0 == 0.0)
            return 0.0;

        if (Double.isInfinite(m0))
            return 1.0;

        // Initial guess: constant-force approximation.
        double q0 = -Math.expm1(-m0);

        for (int i = 0; i < 50; i++)
        {
            double a0 = a0FromQ0CoaleDemeny(q0, gender);
            double nextQ0 = m0 / (1.0 + (1.0 - a0) * m0);

            if (Math.abs(nextQ0 - q0) < 1e-14)
                return nextQ0;

            q0 = nextQ0;
        }

        return q0;
    }

    /*
     * Форумла Coale-Demeny для a0 как функции q0,
     * в "West/North/South" варианте.
     * 
     *     Evgeny M. Andreev, W. Ward Kingkade, 
     *     "Average age at death in infancy and infant mortality level: 
     *     Reconsidering the Coale-Demeny formulas at current levels of low mortality"
     *     Demographic Research, vol 33 (2015), article 13, pp 363-390
     *     https://www.demographic-research.org/volumes/vol33/13/33-13.pdf
     * 
     * "Generally speaking, the lower the level of mortality, the more heavily will infant deaths 
     * be concentrated at the earliest stages of infancy; the influence of the prenatal and perinatal 
     * environment becomes increasingly dominant relative to the postnatal environment".
     * 
     * Для очень низких значений вроде q0 = 0.005 классические формулы Coale-Demeny уже хуже описывают современную младенческую смертность. 
     * Но для преобразования q0 → m0 при столь малом q0 ошибка почти исчезает численно, потому что m0 ≈ q0 при любом разумном a0.
     */
    private static double a0FromQ0CoaleDemeny(double q0, Gender gender)
    {
        q0 = validate_qx(q0);

        boolean highInfantMortality = q0 >= 0.100;

        switch (gender)
        {
        case MALE:
            return highInfantMortality ? 0.330 : 0.0425 + 2.875 * q0;

        case FEMALE:
            return highInfantMortality ? 0.350 : 0.0500 + 3.000 * q0;

        case BOTH:
        default:
            return highInfantMortality ? 0.340 : 0.04625 + 2.9375 * q0;
        }
    }

    /* ============================================================================== */

    /*
     * Convert mortality "qx" value to "mx".
     * Аналитическая формула: mx = −ln(1 − qx).
     * Java: double mx = -Math.log1p(-qx);
     * 
     * Употребима в возрастах >= 1
     */
    public static double qx2mx(double qx) throws Exception
    {
        qx = validate_qx(qx);
        double mx = -Math.log1p(-qx);
        return mx;
    }

    /*
     * Convert mortality "mx" value to "qx".
     * Аналитическая формула: qx = 1 - exp(-mx).
     * Java: double qx = -Math.expm1(-mx); 
     * 
     * Употребима в возрастах >= 1
     */
    public static double mx2qx(double mx) throws Exception
    {
        if (!(mx >= 0 && mx <= MAX_MX))
            throw new Exception("mx value is out of valid range: " + mx);

        double qx = -Math.expm1(-mx);
        return qx;
    }

    /* ============================================================================== */

    /*
     * Convert mortality "qx" value to "mx".
     * Подсчёт грубой вычислительной силой.
     * 
     * Употребима в возрастах >= 1
     */
    public static double qx2mx_old(double qx) throws Exception
    {
        qx = validate_qx(qx);

        if (!useMxTable)
        {
            // linear conversion, acceptable for low values of the argument 
            double mx = qx / (1 - qx / 2);
            return mx;
        }
        else
        {
            double a = Math.pow(1 - qx, 1.0 / NPOINTS_QX2MX);
            double p = 1.0;
            double avg = 0;
            for (int k = 0; k < NPOINTS_QX2MX; k++)
            {
                avg += p;
                p *= a;
            }
            avg /= NPOINTS_QX2MX;

            double mx = qx / avg;
            return mx;
        }
    }

    /*
     * Convert mortality "mx" value to "qx".
     * Подсчёт грубой вычислительной силой.
     * 
     * Употребима в возрастах >= 1
     */
    public static double mx2qx_old(double mx) throws Exception
    {
        if (!useMxTable)
        {
            // linear conversion, acceptable for low values of the argument 
            mx = validate_mx(mx);
            double qx = mx / (1 + mx / 2);
            return qx;
        }
        else
        {
            build_qx2mx_table();
            mx = validate_mx(mx);
            int k1 = 0;
            int k2 = mx2qx_table_mx.length - 1;

            // search position in the table
            for (;;)
            {
                if (mx < mx2qx_table_mx[k1] || mx > mx2qx_table_mx[k2])
                    throw new Exception("Table search error");
                else if (mx == mx2qx_table_mx[k1])
                    return mx2qx_table_qx[k1];
                else if (mx == mx2qx_table_mx[k2])
                    return mx2qx_table_qx[k2];

                int k = (k1 + k2) / 2;
                if (k == k1 || k == k2)
                    break;

                if (mx == mx2qx_table_mx[k])
                    return mx2qx_table_qx[k];
                else if (mx > mx2qx_table_mx[k])
                    k1 = k;
                else
                    k2 = k;
            }

            // search between found table values
            double qx1 = mx2qx_table_qx[k1];
            double qx2 = mx2qx_table_qx[k2];
            for (;;)
            {
                double qx = (qx1 + qx2) / 2;
                double v = qx2mx(qx);

                /*
                 * Если mx m маленький (например 1e-6), то условие abs(mx - v) < 0.0001 
                 * выполнится почти сразу, и qx получится грубо (относительная ошибка 
                 * может быть огромной)
                 */
                // if (Math.abs(mx - v) < 0.0001)
                if (Math.abs(mx - v) < Math.max(1e-12, 1e-8 * mx))
                    return qx;
                else if (mx > v)
                    qx1 = qx;
                else
                    qx2 = qx;
            }
        }

        /*
         * Поведение при qx → 1.
         * При qx = 1 экспоненциальная модель должна давать m=∞,
         * а дискретная даст m ≈ N. 
         * Мы частично обходим это тем, что validate_mx ограничивает максимум mx2qx_table_mx[length-2]. 
         * Практически для реальных таблиц это не проблема (qx никогда не близко к 1 в однолетних возрастах), 
         * но полезно иметь в виду: у нас искусственная “потолочная” смертность, заданная NPOINTS.
         */
    }

    private static synchronized void build_qx2mx_table() throws Exception
    {
        if (mx2qx_table_mx == null)
        {
            mx2qx_table_mx = new double[MX2QX_TABLE_SIZE];
            mx2qx_table_qx = new double[MX2QX_TABLE_SIZE];
            double step = 1.0 / (MX2QX_TABLE_SIZE - 1);
            for (int k = 0; k < MX2QX_TABLE_SIZE; k++)
            {
                mx2qx_table_qx[k] = k * step;
                mx2qx_table_mx[k] = qx2mx(mx2qx_table_qx[k]);
            }
        }
    }

    /* ============================================================================== */

    private static double validate_qx(double qx) throws IllegalArgumentException
    {
        if (qx >= 0 && qx <= 1.0)
            return qx;
        throw new IllegalArgumentException("qx value is out of valid range");
    }

    private static double validate_mx(double mx) throws IllegalArgumentException
    {
        if (!useMxTable)
        {
            if (mx >= 0 && mx <= MAX_MX)
                return mx;
        }
        else
        {
            // restrict the range a bit
            if (mx >= 0 && mx <= mx2qx_table_mx[mx2qx_table_mx.length - 2])
                return mx;
        }

        throw new IllegalArgumentException("mx value is out of valid range: " + mx);
    }

    /* ============================================================================== */

    public static double[] qx2mx(double[] qx, Gender gender) throws Exception
    {
        double[] mx = new double[qx.length];
        for (int k = 0; k < qx.length; k++)
            mx[k] = qx2mx(qx[k], gender, k);
        return mx;
    }

    public static double[] mx2qx(double[] mx, Gender gender) throws Exception
    {
        double[] qx = new double[mx.length];
        for (int k = 0; k < qx.length; k++)
            qx[k] = mx2qx(mx[k], gender, k);
        return qx;
    }

    public static Bin[] qx2mx(Bin[] bins, Gender gender) throws Exception
    {
        bins = Bins.clone(bins);
        for (Bin bin : bins)
            bin.avg = qx2mx(bin.avg, gender, bin.mid_x);
        return bins;
    }

    public static Bin[] mx2qx(Bin[] bins, Gender gender) throws Exception
    {
        bins = Bins.clone(bins);
        for (Bin bin : bins)
            bin.avg = mx2qx(bin.avg, gender, bin.mid_x);
        return bins;
    }

    public static Bin[] proqx2mx(Bin[] bins, Gender gender) throws Exception
    {
        return qx2mx(Bins.multiply(bins, 0.001), gender);
    }

    public static double[] proqx2mx(double[] proqx, Gender gender) throws Exception
    {
        return qx2mx(Util.multiply(proqx, 0.001), gender);
    }
}
