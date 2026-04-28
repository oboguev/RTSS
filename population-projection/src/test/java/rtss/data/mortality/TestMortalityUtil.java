package rtss.data.mortality;

import rtss.data.selectors.Gender;
import rtss.util.Util;

public class TestMortalityUtil
{
    public static void main(String[] args)
    {
        TestMortalityUtil self = new TestMortalityUtil();

        try
        {
            self.test_1();
            self.test_2();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void test_1() throws Exception
    {
        Util.out("");
        Util.out("qx => mx mx_old => qx");
        Util.out("");

        for (double qx = 0; qx <= 1.0; qx += 0.1)
        {
            double qxe = qx;
            if (qxe >= 0.999999999999)
                qxe = 0.999;
            double mx = MortalityUtil.qx2mx(qxe);
            double mx_old = MortalityUtil.qx2mx_old(qxe);
            if (mx <= MortalityUtil.MAX_MX)
            {
                double qx2 = MortalityUtil.mx2qx(mx);
                Util.out(String.format("%.4f => %.4f %.4f => %.4f", qxe, mx, mx_old, qx2));
            }
            else
            {
                Util.out(String.format("%.4f => %.4f %.4f", qxe, mx, mx_old));
            }
        }
    }

    private void test_2() throws Exception
    {
        test_2(Gender.MALE);
        test_2(Gender.FEMALE);
        test_2(Gender.BOTH);
    }

    private void test_2(Gender gender) throws Exception
    {
        Util.out("");
        Util.out(gender.name().toUpperCase() + " q0 => m0 m0_const => q0");
        Util.out("");

        for (double qx = 0; qx <= 1.0; qx += 0.05)
        {
            double qxe = qx;
            if (qxe >= 0.999999999999)
                qxe = 0.999;

            double mx= MortalityUtil.qx2mx(qxe, gender, 0);
            double mx_const = MortalityUtil.qx2mx(qxe);

            if (mx <= MortalityUtil.MAX_MX)
            {
                double qx2 = MortalityUtil.mx2qx(mx, gender, 0);
                Util.out(String.format("%.4f => %.4f %.4f => %.4f", qxe, mx, mx_const, qx2));
            }
            else
            {
                Util.out(String.format("%.4f => %.4f %.4f", qxe, mx, mx_const));
            }
        }
    }
}
