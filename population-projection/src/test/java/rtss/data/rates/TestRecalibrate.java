package rtss.data.rates;

import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestRecalibrate
{
    /* рождаемость и смертность в 1937-1939 гг. в нормировке на середину года (АДХ-СССР, стр. 120) */
    private static final double CBR_1937_MIDYEAR = 39.9;
    private static final double CDR_1937_MIDYEAR = 21.7;

    private static final double CBR_1938_MIDYEAR = 39.0;
    private static final double CDR_1938_MIDYEAR = 20.9;

    private static final double CBR_1939_MIDYEAR = 40.0;
    private static final double CDR_1939_MIDYEAR = 20.1;

    public static void main(String[] args)
    {
        try
        {
            TestRecalibrate self = new TestRecalibrate();
            self.test_1(false);
            self.test_1(true);
            self.test_2();

            Util.out("");
            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void test_1(boolean linear) throws Exception
    {
        test_1(linear, 1937, CBR_1937_MIDYEAR, CDR_1937_MIDYEAR);
        test_1(linear, 1938, CBR_1938_MIDYEAR, CDR_1938_MIDYEAR);
        test_1(linear, 1939, CBR_1939_MIDYEAR, CDR_1939_MIDYEAR);
    }

    private void test_1(boolean linear, int year, double cbr, double cdr) throws Exception
    {
        Rates rm = new Rates(cbr, cdr);
        Rates re, rm2;
        
        Util.out("");

        if (linear)
        {
            Util.out("Linear " + year);
            re = Recalibrate.m2e_linear(rm);
            rm2 = Recalibrate.e2m_linear(re);
        }
        else
        {
            Util.out("Constant-rate " + year);
            re = Recalibrate.m2e(rm);
            rm2 = Recalibrate.e2m(re);
        }

        Util.out(String.format("%.3f %.3f", rm.cbr, rm.cdr));
        Util.out(String.format("%.3f %.3f", re.cbr, re.cdr));
        Util.out(String.format("%.3f %.3f", rm2.cbr, rm2.cdr));

        Util.checkSame(rm.cbr, rm2.cbr);
        Util.checkSame(rm.cdr, rm2.cdr);

        if (!(re.cbr >= rm.cbr && re.cdr >= rm.cdr))
            throw new Exception("Invalid value");

    }

    private void test_2() throws Exception
    {
        double vm = 40.0;
        double ve = Recalibrate.m2e(Area.RSFSR, 1939, vm);
        double vm2 = Recalibrate.e2m(Area.RSFSR, 1939, ve);
        Util.checkSame(vm, vm2);
        if (!(ve >= vm))
            throw new Exception("Invalid value");

        Util.out("");
        Util.out("test_2");
        Util.out(String.format("%.3f => %.3f => %.3f", vm, ve, vm2));
    }
}
