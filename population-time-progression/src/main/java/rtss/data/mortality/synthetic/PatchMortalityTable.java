package rtss.data.mortality.synthetic;

import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class PatchMortalityTable
{
    /*
     * Построить таблицу смертности с изменённой младенческой смертностью
     */
    public static CombinedMortalityTable patchInfantMortalityRate(final CombinedMortalityTable mt, double cdr, String addComment) throws Exception
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

        patchInfantMortalityRate(cmt, mt, factor, Locality.TOTAL, addComment);
        patchInfantMortalityRate(cmt, mt, factor, Locality.RURAL, addComment);
        patchInfantMortalityRate(cmt, mt, factor, Locality.URBAN, addComment);

        return cmt;
    }

    private static void patchInfantMortalityRate(CombinedMortalityTable cmt, CombinedMortalityTable mt, double factor, Locality locality,
            String addComment) throws Exception
    {
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.MALE, addComment);
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.FEMALE, addComment);
        patchInfantMortalityRate(cmt, mt, factor, locality, Gender.BOTH, addComment);
    }

    private static void patchInfantMortalityRate(CombinedMortalityTable cmt, CombinedMortalityTable mt, double factor, Locality locality,
            Gender gender, String addComment) throws Exception
    {
        SingleMortalityTable smt = mt.getSingleTable(locality, gender);

        if (smt == null)
            return;

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

    public static enum PatchOpcode
    {
        Multiply, MultiplyWithDecay
    }

    public static class PatchInstruction
    {
        public PatchOpcode opcode;
        public int age1;
        public int age2;
        public double scale;
        public double scale2;
        
        public PatchInstruction(PatchOpcode opcode, int age1, int age2, double scale)
        {
            this.opcode = opcode;
            this.age1 = age1;
            this.age2 = age2;
            this.scale = scale;
        }

        public PatchInstruction(PatchOpcode opcode, int age1, int age2, double scale, double scale2)
        {
            this.opcode = opcode;
            this.age1 = age1;
            this.age2 = age2;
            this.scale = scale;
            this.scale2 = scale2;
        }
    }

    /*
     * Построить таблицу смертности с изменениями по инструкции
     */
    public static CombinedMortalityTable patch(CombinedMortalityTable mt,
            List<PatchInstruction> instructions,
            String addComment) throws Exception
    {
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        String comment = mt.comment();
        if (comment != null)
            comment += ", ";
        else
            comment = "";
        cmt.comment(comment + addComment);

        patch(cmt, mt, instructions, Locality.TOTAL, addComment);
        patch(cmt, mt, instructions, Locality.RURAL, addComment);
        patch(cmt, mt, instructions, Locality.URBAN, addComment);

        return cmt;
    }

    private static void patch(CombinedMortalityTable cmt,
            CombinedMortalityTable mt,
            List<PatchInstruction> instructions,
            Locality locality,
            String addComment) throws Exception
    {
        patch(cmt, mt, instructions, locality, Gender.MALE, addComment);
        patch(cmt, mt, instructions, locality, Gender.FEMALE, addComment);
        patch(cmt, mt, instructions, locality, Gender.BOTH, addComment);
    }

    private static void patch(CombinedMortalityTable cmt,
            CombinedMortalityTable mt,
            List<PatchInstruction> instructions,
            Locality locality,
            Gender gender,
            String addComment) throws Exception
    {
        SingleMortalityTable smt = mt.getSingleTable(locality, gender);

        if (smt == null)
            return;

        double[] qx = smt.qx();
        
        patch(qx, instructions);

        String source = smt.source();
        if (source != null)
            source += ", ";
        else
            source = "";

        smt = SingleMortalityTable.from_qx(source + addComment, qx);
        cmt.setTable(locality, gender, smt);
    }
    
    private static void patch(double[] qx, List<PatchInstruction> instructions)
    {
        for (PatchInstruction instruction: instructions)
            patch(qx, instruction);
    }

    private static void patch(double[] qx, PatchInstruction instruction)
    {
        switch (instruction.opcode)
        {
        case Multiply:
            patchMultiply(qx, instruction);
            break;
        
        case MultiplyWithDecay:
            patchMultiplyWithDecay(qx, instruction);;
            break;
        }
    }

    private static void patchMultiply(double[] qx, PatchInstruction instruction)
    {
        for (int age = instruction.age1; age <= instruction.age2; age++)
            qx[age] *= instruction.scale;
    }

    private static void patchMultiplyWithDecay(double[] qx, PatchInstruction instruction)
    {
        double a = (instruction.scale2 - instruction.scale) / (instruction.age2 - instruction.age1);
        double b = instruction.scale - a * instruction.age1;
        
        for (int age = instruction.age1; age <= instruction.age2; age++)
        {
            double scale = a * age + b;
            qx[age] *= scale;
        }
    }
}
