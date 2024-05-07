package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.MortalityUtil;
import rtss.external.Osier.Osier;
import rtss.external.Osier.OsierCall;
import rtss.external.Osier.OsierScript;
import rtss.util.Util;

public class OsierTask
{
    private OsierScript osier = new OsierScript();
    private OsierCall ocall = Osier.ocall();

    private OsierTask() throws Exception
    {
        // ocall.enableLocalLog(true);
    }

    public static double[] mortality(Bin[] bins, String datasetName, String method, int ppy) throws Exception
    {
        return new OsierTask().do_mortality(bins, datasetName, method, ppy);
    }

    public static double[] population(Bin[] bins, String datasetName, String method, int ppy) throws Exception
    {
        return new OsierTask().do_population(bins, datasetName, method, ppy);
    }

    private double[] do_mortality(Bin[] bins, String datasetName, String method, int ppy) throws Exception
    {
        String sc, reply;
        boolean use_mx = true;
        boolean visible = true;

        /* convert pro mille values from qx to mx */
        Bin[] xbins = Bins.multiply(bins, 0.001);
        if (use_mx)
            xbins = MortalityUtil.qx2mx(xbins);

        /* if not called here, will be defaulted to the setting in rtss-config.yml */
        ocall.setDefaultStartupScript(visible);

        osier.clear_worksheet();
        osier.createBaseMortalityObject(xbins, datasetName, use_mx);
        sc = osier.getScript();
        reply = ocall.execute(sc, true);
        osier.replyBaseMortalityObject(reply);

        osier.newScript();
        osier.modifyBaseMortalityObject(method);
        sc = osier.getScript();
        reply = ocall.execute(sc, true);
        osier.replyModifyBaseMortalityObject(reply);

        double[] curve = getCurve("DeathProb", bins, ppy);
        if (use_mx)
            curve = MortalityUtil.mx2qx(curve);
        // ocall.stop();
        curve = Util.multiply(curve, 1000.0);
        return curve;
    }

    private double[] do_population(Bin[] bins, String datasetName, String method, int ppy) throws Exception
    {
        String sc, reply;
        boolean visible = true;

        /* if not called here, will be defaulted to the setting in rtss-config.yml */
        ocall.setDefaultStartupScript(visible);

        osier.clear_worksheet();
        osier.createBasePopulationObject(bins, datasetName);
        sc = osier.getScript();
        reply = ocall.execute(sc, true);
        osier.replyBasePopulationObject(reply);

        osier.newScript();
        osier.modifyBasePopulationObject(method);
        sc = osier.getScript();
        reply = ocall.execute(sc, true);
        osier.replyModifyBasePopulationObject(reply);

        double[] curve = getCurve("Number", bins, ppy);
        // ocall.stop();
        curve = Util.multiply(curve, 1000.0);
        return curve;
    }

    private double[] getCurve(String func, Bin[] bins, int ppy) throws Exception
    {
        return getCurve(func, Bins.ppy_x(bins, ppy), ppy == 1);
    }

    private double[] getCurve(String func, double[] x, boolean isIntegerX) throws Exception
    {
        if (x.length <= 190)
            return getCurveChunk(func, x, isIntegerX);

        double[] result = null;

        int chunk = 0;
        for (int ix = 0; ix < x.length; ix += chunk)
        {
            chunk = x.length - ix;
            if (chunk >= 200)
                chunk = Math.min(chunk, 100);

            double[] xx = Util.splice(x, ix, ix + chunk - 1);
            double[] yy = getCurveChunk(func, xx, isIntegerX);
            if (result == null)
                result = yy;
            else
                result = Util.concat(result, yy);

        }

        return result;
    }

    private double[] getCurveChunk(String func, double[] x, boolean isIntegerX) throws Exception
    {
        double[] y = new double[x.length];
        osier.newScript();
        if (func.equals("Number"))
        {
            int interval = 1;
            osier.twoArgFunction(func, x, isIntegerX, interval);
        }
        else
        {
            osier.oneArgFunction(func, x, isIntegerX);
        }
        String sc = osier.getScript();
        String reply = ocall.execute(sc, true);
        int ix = 0;

        for (String line : reply.split("\n"))
        {
            if (line.startsWith(func + " "))
            {
                line = Util.stripStart(line, func + " ").trim();
                String[] sa = line.split(": ");
                if (sa.length != 2)
                    throw new Exception("Unexpected response from Osier");
                double xv = Double.parseDouble(sa[0]);
                double yv = Double.parseDouble(sa[1]);
                if (ix >= x.length)
                    throw new Exception("Unexpected response from Osier");
                if (Util.differ(x[ix], xv))
                    throw new Exception("Unexpected response from Osier (mismatching x)");
                y[ix++] = yv;
            }
        }

        if (ix != x.length)
            throw new Exception("Unexpected incomplete response from Osier");

        return y;
    }
}