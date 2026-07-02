package rtss.rosbris1959;

import java.io.File;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.mortality.synthetic.BuildSingleTable;
import rtss.data.mortality.synthetic.BuildSingleTable.BuildMortalityCurveOptions;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class ExportMortalityTables
{
    public static void main(String[] args)
    {
        try
        {
            new ExportMortalityTables().do_main();
            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private void do_main() throws Exception
    {
        RosBris1959DeathRates drAll = RosBris1959DeathRates.load().forCause(0);
        
        for (int year = 1959; year <= 1988; year++)
        {
            Util.out("Creating table for " + year);

            RosBris1959DeathRates dr = drAll.forYear(year);
            SingleMortalityTable m_smt = build(dr, year, Gender.MALE);
            SingleMortalityTable f_smt = build(dr, year, Gender.FEMALE);
            
            CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
            cmt.setTable(Locality.TOTAL, Gender.MALE, m_smt);
            cmt.setTable(Locality.TOTAL, Gender.FEMALE, f_smt);

            String comment = "# Таблица построена модулем " + ExportMortalityTables.class.getCanonicalName() + " по данным в РосБРИС для РСФСР " + year;
            String dir = "C:\\@\\zzzz\\RosBRIS\\" + year;
            new File(dir).mkdirs();
            cmt.saveTable(dir, comment);
            Util.out("Exported table for " + year);
        }
    }
    
    private SingleMortalityTable build(RosBris1959DeathRates dr, int year, Gender gender) throws Exception
    {
        dr = dr.forGender(gender);
        Bin[] bins = dr.asBins();
        bins = Bins.multiply(bins, 1000.0);
        
        BuildMortalityCurveOptions options = new BuildMortalityCurveOptions();
        options.curveVerifierOptions().maxAbsDifference(0.01);

        String title = String.format("РОСБРИС-%s-%s %s %s", Area.RSFSR.name(), year, Locality.TOTAL.name(), gender.name());
        SingleMortalityTable smt = BuildSingleTable.makeSingleTable(bins, null, title, null, options);
        return smt;
    }
}
