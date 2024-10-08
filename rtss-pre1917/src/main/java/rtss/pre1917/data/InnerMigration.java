package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

public class InnerMigration
{
    public static class InnerMigrationAmount
    {
        public InnerMigrationAmount(String tname)
        {
            this.tname = tname;
        }

        @SuppressWarnings("unused")
        private final String tname;
        Map<Integer, Long> year2inflow = new HashMap<>();
        Map<Integer, Long> year2outflow = new HashMap<>();;
    }

    private Map<String, InnerMigrationAmount> tname2ima = new HashMap<>();

    public void setInFlow(String tname, int year, long amount) throws Exception
    {
        setValue(tname, makeInnerMigrationAmount(tname).year2inflow, year, amount);
    }

    public void setOutFlow(String tname, int year, long amount) throws Exception
    {
        setValue(tname, makeInnerMigrationAmount(tname).year2outflow, year, amount);
    }

    private InnerMigrationAmount makeInnerMigrationAmount(String tname)
    {
        InnerMigrationAmount ima = tname2ima.get(tname);

        if (ima == null)
        {
            ima = new InnerMigrationAmount(tname);
            tname2ima.put(tname, ima);
        }

        return ima;

    }

    private void setValue(String tname, Map<Integer, Long> year2flow, int year, long amount) throws Exception
    {
        if (year2flow.containsKey(year))
        {
            year2flow.put(year, amount + year2flow.get(year));
        }
        else
        {
            year2flow.put(year, amount);
        }
    }

    /* ==================================================================== */

    /*
     * Yearly data as published is [1896...1914]. 
     * Coarse data is 1896-1910, 1911-1915, 1916.
     * Yearly values for 1915 and 1916 can be derived from coarse data,
     * and both sets can be cross-validated against each other.
     */

    public static class CoarseData
    {
        public CoarseData(String tname, int y1, int y2)
        {
            this.tname = tname;
            this.y1 = y1;
            this.y2 = y2;
        }

        @SuppressWarnings("unused")
        private final String tname;
        private final int y1;
        private final int y2;

        public Long inFlow = null;
        public Long outFlow = null;

        public void addInFlow(Long v)
        {
            if (v != null)
            {
                inFlow = (inFlow != null) ? (inFlow + v) : v;
            }
        }

        public void addOutFlow(Long v)
        {
            if (v != null)
            {
                outFlow = (outFlow != null) ? (outFlow + v) : v;
            }
        }
    }

    private class CoarseDataHolder extends HashMap<String, CoarseData>
    {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        public CoarseData get(String tname, int y1, int y2)
        {
            String key = coarseKey(tname, y1, y2);
            return get(key);
        }

        public CoarseData ondemand(String tname, int y1, int y2)
        {
            String key = coarseKey(tname, y1, y2);
            CoarseData v = get(key);
            if (v == null)
            {
                v = new CoarseData(tname, y1, y2);
                put(key, v);
            }
            return v;
        }

        private String coarseKey(String tname, int y1, int y2)
        {
            return String.format("%s-%d-%s", tname, y1, y2);
        }
    }

    private final CoarseDataHolder coarseDataHolder = new CoarseDataHolder();

    public void setInFlowCoarse(String tname, Long amount, int y1, int y2) throws Exception
    {
        coarseDataHolder.ondemand(tname, y1, y2).addInFlow(amount);
    }

    public void setOutFlowCoarse(String tname, Long amount, int y1, int y2) throws Exception
    {
        coarseDataHolder.ondemand(tname, y1, y2).addOutFlow(amount);
    }

    private final boolean domsg = Util.False;

    public void build() throws Exception
    {
        if (domsg)
            Util.err("=== Building InnerMigration ===");

        for (CoarseData coarse : coarseDataHolder.values())
        {
            if (coarse.y1 == 1916 && coarse.y2 == 1916)
            {
                if (coarse.inFlow != null)
                    setInFlow(coarse.tname, 1916, coarse.inFlow);
                if (coarse.outFlow != null)
                    setOutFlow(coarse.tname, 1916, coarse.outFlow);
            }
            else if (coarse.y1 == 1896 && coarse.y2 == 1910)
            {
                long v = sumInFlow(coarse.tname, 1896, 1910);
                if (null2zero(coarse.inFlow) != v && domsg)
                {
                    String msg = String
                            .format("Sum of yearly inflow migration data for 1896-1910 [%s] differs from coarse value: %,d vs. %,d, difference: %,d",
                                    coarse.tname, v, null2zero(coarse.inFlow), Math.abs(v - null2zero(coarse.inFlow)));
                    Util.err(msg);
                }

                v = sumOutFlow(coarse.tname, 1896, 1910);
                if (null2zero(coarse.outFlow) != v && domsg)
                {
                    String msg = String
                            .format("Sum of yearly outflow migration data for 1896-1910 [%s] differs from coarse value: %,d vs. %,d, difference: %,d",
                                    coarse.tname, v, null2zero(coarse.outFlow), Math.abs(v - null2zero(coarse.outFlow)));
                    Util.err(msg);
                }
            }
            else if (coarse.y1 == 1911 && coarse.y2 == 1915)
            {
                long v = sumInFlow(coarse.tname, 1911, 1914);
                if (v > null2zero(coarse.inFlow) && domsg)
                {
                    String msg = String
                            .format("Sum of yearly inflow migration data for 1911-1914 [%s] exceeds coarse value for 1911-1915: %,d vs. %,d, difference: %,d",
                                    coarse.tname, v, coarse.inFlow, Math.abs(v - coarse.inFlow));
                    Util.err(msg);
                }
                else
                {
                    setInFlow(coarse.tname, 1915, null2zero(coarse.inFlow) - v);
                }

                v = sumOutFlow(coarse.tname, 1911, 1914);
                if (v > null2zero(coarse.outFlow) && domsg)
                {
                    String msg = String
                            .format("Sum of yearly outflow migration data for 1911-1914 [%s] exceeds coarse value for 1911-1915: %,d vs. %,d, difference: %,d",
                                    coarse.tname, v, null2zero(coarse.outFlow), Math.abs(v - null2zero(coarse.outFlow)));
                    Util.err(msg);
                }
                else
                {
                    setOutFlow(coarse.tname, 1915, null2zero(coarse.outFlow) - v);
                }
            }
            else
            {
                throw new Exception("Unxpected coarse data time range");
            }
        }

        /* ======================================== */

        // make sure yearly balance across all migrations is near-zero

        if (domsg)
        {
            Util.out("Migration inflow-outflow balance per year");
            for (int year = 1896; year <= 1916; year++)
            {
                long inflow = sumInFlow(year);
                long outflow = sumOutFlow(year);
                Util.out(String.format("%d %,d %,d", year, inflow, outflow));
            }
        }
    }

    private long sumInFlow(String tname, int y1, int y2)
    {
        long v = 0;
        for (int year = y1; year <= y2; year++)
            v += inFlow(tname, year);
        return v;
    }

    private long sumOutFlow(String tname, int y1, int y2)
    {
        long v = 0;
        for (int year = y1; year <= y2; year++)
            v += outFlow(tname, year);
        return v;
    }

    private long null2zero(Long v)
    {
        return v == null ? 0 : v;
    }

    private long sumInFlow(int year)
    {
        long v = 0;
        for (String tname : tname2ima.keySet())
        {
            InnerMigrationAmount ima = tname2ima.get(tname);
            v += null2zero(ima.year2inflow.get(year));
        }
        return v;
    }

    private long sumOutFlow(int year)
    {
        long v = 0;
        for (String tname : tname2ima.keySet())
        {
            InnerMigrationAmount ima = tname2ima.get(tname);
            v += null2zero(ima.year2outflow.get(year));
        }
        return v;
    }

    /* ==================================================================== */

    public long inFlow(String tname, int year)
    {
        MergeDescriptor md = MergePost1897Regions.find(tname);

        if (md == null)
        {
            InnerMigrationAmount ima = tname2ima.get(mapTerritoryName(tname));
            if (ima == null)
                return 0;

            Long v = ima.year2inflow.get(year);
            if (v == null)
                v = 0L;

            return v;
        }
        else
        {
            long v = 0;
            for (String xtn : md.parentWithChildren())
            {
                v += inFlow(xtn, year);
            }
            return v;
        }
    }

    public long outFlow(String tname, int year)
    {
        MergeDescriptor md = MergePost1897Regions.find(tname);

        if (md == null)
        {
            InnerMigrationAmount ima = tname2ima.get(mapTerritoryName(tname));
            if (ima == null)
                return 0;

            Long v = ima.year2outflow.get(year);
            if (v == null)
                v = 0L;

            return v;
        }
        else
        {
            long v = 0;
            for (String xtn : md.parentWithChildren())
            {
                v += outFlow(xtn, year);
            }
            return v;
        }
    }

    public long saldo(String tname, int year)
    {
        return inFlow(tname, year) - outFlow(tname, year);
    }

    private String mapTerritoryName(String tname)
    {
        for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
        {
            if (tname.equals(md.combined) && md.parent != null)
            {
                tname = md.parent;
                break;
            }
        }

        return tname;
    }
}
