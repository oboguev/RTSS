package rtss.data.mortality.synthetic;

import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.struct.PopulationByLocality;
import rtss.util.Util;

/*
 * Перестроить таблицу смертности так, чтобы она давала требуемую смертность при заданной рождаемости.
 * Перестройка достигается изменением множителя @scale в последнем элементе @instructions,
 * который обыкновенно имеет тип Multiply.
 * 
 * При выходе последний элемент @instructions содержит в поле @scale найденное значение масштабного
 * множителя.
 */
public class MatchMortalityTable
{
    public static CombinedMortalityTable match(
            final CombinedMortalityTable mt,
            final PopulationByLocality p,
            List<PatchInstruction> instructions,
            double cbr,
            double cdr,
            String addComment) throws Exception
    {
        final double cdr_tolerance = cdr / 5000;
        
        if (instructions.size() < 1)
            throw new IllegalArgumentException("instructions size is 0");
        PatchInstruction instruction = instructions.get(instructions.size() - 1);
        if (Util.False && instruction.opcode != PatchOpcode.Multiply)
            throw new IllegalArgumentException("laast instruction is not Multiply");
        
        double scale1 = 1.0; 
        instruction.scale = scale1;
        
        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt, instructions, addComment);
        double xcdr = new EvalMortalityRate().eval(xmt, p, null, cbr);
        if (Math.abs(xcdr - cdr) < cdr_tolerance)
            return xmt;
        
        if (xcdr < cdr)
        {
            /*
             * Повышать смертность
             */
            for (;;)
            {
                instruction.scale *= 2;
                xmt = PatchMortalityTable.patch(mt, instructions, addComment);
                xcdr = new EvalMortalityRate().eval(xmt, p, null, cbr);
                if (xcdr > cdr)
                    break;
            }
        }
        else // if (xcdr > cdr) 
        {
            /*
             * Понижаать смертность
             */
            
            for (;;)
            {
                instruction.scale /= 2;
                xmt = PatchMortalityTable.patch(mt, instructions, addComment);
                xcdr = new EvalMortalityRate().eval(xmt, p, null, cbr);
                if (xcdr < cdr)
                    break;
            }
        }
        
        /*
         * Упорядочить scale1 и scale2
         */
        double scale2 = instruction.scale;
        if (scale1 > scale2)
        {
            double x = scale1;
            scale1 = scale2;
            scale2 = x;
        }
        
        /*
         * Двоичный поиск между scale1 и scale2
         */
        for (;;)
        {
            double scalex = (scale1 + scale2) / 2;

            instruction.scale = scalex;
            xmt = PatchMortalityTable.patch(mt, instructions, addComment);
            xcdr = new EvalMortalityRate().eval(xmt, p, null, cbr);

            if (Math.abs(xcdr - cdr) < cdr_tolerance)
                return xmt;
            
            if (xcdr > cdr)
            {
                scale2 = scalex;
            }
            else
            {
                scale1 = scalex;
            }
        }
    }
}
