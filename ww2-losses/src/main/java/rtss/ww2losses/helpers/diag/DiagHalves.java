package rtss.ww2losses.helpers.diag;

import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

public class DiagHalves
{
    public void compare(HalfYearEntries<HalfYearEntry> halves1, HalfYearEntries<HalfYearEntry> halves2) throws Exception
    {
        Util.assertion(halves1.size() == halves2.size());
        
        for (int k = 0; k < halves1.size(); k++)
        {
            HalfYearEntry he1 = halves1.get(k);
            HalfYearEntry he2 = halves2.get(k);
            Util.assertion(he1.id().equals(he2.id()));
            
            try
            {
                compare(he1, he2);
            }
            catch (Exception ex)
            {
                throw new Exception("while comparing " + he1.id(), ex);
            }
        }
    }

    private void compare(HalfYearEntry he1, HalfYearEntry he2) throws Exception
    {
        Util.checkSame(he1.expected_nonwar_deaths, he2.expected_nonwar_deaths);
        Util.checkSame(he1.p_nonwar_without_births.sum(Gender.MALE), he2.p_nonwar_without_births.sum(Gender.MALE));
        Util.checkSame(he1.p_nonwar_without_births.sum(Gender.FEMALE), he2.p_nonwar_without_births.sum(Gender.FEMALE));

        Util.checkSame(he1.expected_nonwar_births, he2.expected_nonwar_births);
        Util.checkSame(he1.p_nonwar_with_births.sum(Gender.MALE), he2.p_nonwar_with_births.sum(Gender.MALE));
        Util.checkSame(he1.p_nonwar_with_births.sum(Gender.FEMALE), he2.p_nonwar_with_births.sum(Gender.FEMALE));
    }
}
