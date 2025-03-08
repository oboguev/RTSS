package rtss.ww2losses.helpers.diag;

import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXYSPlineBasic;

public class DiagHelper
{
    private final AreaParameters ap;
    private final HalfYearEntries<HalfYearEntry> halves;

    public DiagHelper(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves)
    {
        this.ap = ap;
        this.halves = halves;
    }
    
    public void showEarlyAges() throws Exception
    {
        for (HalfYearEntry he : halves)
        {
            double offset = he.offset_start1941();
            if (offset >= 0.5)
            {
                int nd1 = years2days(offset - 0.5);
                int nd2 = years2days(offset + 1);
                double[] v =he.p_nonwar_without_births.asArray(Locality.TOTAL, Gender.MALE);
                v = Util.splice(v, nd1, nd2);
                ChartXYSPlineBasic.display("Муж. население " + ap.area + " в возрасте 4.5 - 6 @ 1946 на " + he.id(), v);
            }
        }
    }
}
