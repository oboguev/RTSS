package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class PatchMortalityTable
{
    public static CombinedMortalityTable patchInfantMortalityRate(CombinedMortalityTable mt, double cdr, String addComment) throws Exception
    {
        double[] qx = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();
        double factor = (cdr / 1000.0) / qx[0];
        
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        String comment = mt.comment();
        if (comment != null)
            comment += ", ";
        else 
            comment = "";
        cmt.comment(comment + addComment);
        
        patchInfantMortalityRate(cmt, mt, factor, Locality.RURAL, addComment);
        patchInfantMortalityRate(cmt, mt, factor, Locality.TOTAL, addComment);
        patchInfantMortalityRate(cmt, mt, factor, Locality.URBAN, addComment);

        return cmt;
    }
    
    private static void patchInfantMortalityRate(CombinedMortalityTable cmt, CombinedMortalityTable mt, double factor, Locality locality, String addComment) throws Exception
    {
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.MALE, addComment);
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.FEMALE, addComment);
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.BOTH, addComment);
    }

    private static void patchInfantMortalityRate(CombinedMortalityTable cmt, CombinedMortalityTable mt, double factor, Locality locality, Gender gender, String addComment) throws Exception
    {
        SingleMortalityTable smt = mt.getSingleTable(locality, gender);
        double[] qx = smt.qx();
        qx[0] *= factor;

        String source = smt.source();
        if (source != null)
            source += ", ";
        else 
            source = "";
        
        smt = SingleMortalityTable.from_qx(source + addComment, qx);
        cmt.setTable(locality, gender, smt);
    }
}
