package rtss.data.population.synthetic;

import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptionsByGender;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.util.plot.CaptureImages;

public class TestPopulationADH
{
    public static void main(String[] args)
    {
        try
        {
            if (Util.True)
            {
                InterpolationOptionsByGender options = new InterpolationOptionsByGender();
                options.both().debugSecondaryRefineYearlyAges(true);
                
                PopulationADH.getPopulationByLocality(Area.USSR, 1926, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1941, options);
                Util.noop();
            }

            if (Util.True)
            {
                CaptureImages.capture("c:\\@@capture\\xxx", "USSR - ", 2400, 1500);

                PopulationADH.getPopulationByLocality(Area.USSR, 1926);
                PopulationADH.getPopulationByLocality(Area.USSR, 1927);
                PopulationADH.getPopulationByLocality(Area.USSR, 1937);
                PopulationADH.getPopulationByLocality(Area.USSR, 1938);
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1938");
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1946");
                PopulationADH.getPopulationByLocality(Area.USSR, 1940);
                PopulationADH.getPopulationByLocality(Area.USSR, 1941);
                PopulationADH.getPopulationByLocality(Area.USSR, 1946);
                PopulationADH.getPopulationByLocality(Area.USSR, 1947);

                CaptureImages.stop();
            }

            if (Util.True)
            {
                CaptureImages.capture("c:\\@@capture\\xxx", "RSFSR - ", 2400, 1500);

                for (int year = 1927; year <= 1959; year++)
                {
                    if (year >= 1942 && year <= 1945)
                        continue;
                    PopulationADH.getPopulationByLocality(Area.RSFSR, year);
                }

                CaptureImages.stop();
            }

            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
