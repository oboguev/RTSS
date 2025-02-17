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
            InterpolationOptionsByGender options = new InterpolationOptionsByGender().allowCache(false);

            if (Util.False)
            {
                options.both().displayChart(true).debugSecondaryRefineYearlyAges(true).allowChartMinorClipping(true).extra("chart-spline").extra("chart-csasra");
                
                options.both().secondaryRefineYearlyAgesSmoothness(0.70).subtitle("0.70");
                PopulationADH.getPopulationByLocality(Area.RSFSR, 1927, options);

                options.both().secondaryRefineYearlyAgesSmoothness(0.90).subtitle("0.90");
                // PopulationADH.getPopulationByLocality(Area.RSFSR, 1927, options);

                options.both().secondaryRefineYearlyAgesSmoothness(0.94).subtitle("0.94");
                // PopulationADH.getPopulationByLocality(Area.RSFSR, 1927, options);

                options.both().secondaryRefineYearlyAgesSmoothness(0.98).subtitle("0.98");
                //PopulationADH.getPopulationByLocality(Area.RSFSR, 1927, options);

                // PopulationADH.getPopulationByLocality(Area.USSR, 1926, options);
                // PopulationADH.getPopulationByLocality(Area.USSR, 1941, options);
                Util.noop();
            }

            options = new InterpolationOptionsByGender().allowCache(false);
            options.both().secondaryRefineYearlyAgesSmoothness(0.95).allowChartMinorClipping(true).extra("chart-spline").extra("chart-csasra");

            if (Util.True)
            {
                CaptureImages.capture("c:\\@@capture\\xxx", "USSR - ", " 0.95", 2400, 1500);

                PopulationADH.getPopulationByLocality(Area.USSR, 1926, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1927, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1937, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1938, options);
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1938", options);
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1946", options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1940, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1941, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1946, options);
                PopulationADH.getPopulationByLocality(Area.USSR, 1947, options);

                CaptureImages.stop();
            }

            if (Util.True)
            {
                CaptureImages.capture("c:\\@@capture\\xxx", "RSFSR - ", " 0.95", 2400, 1500);

                for (int year = 1927; year <= 1959; year++)
                {
                    if (year >= 1942 && year <= 1945)
                        continue;
                    PopulationADH.getPopulationByLocality(Area.RSFSR, year, options);
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
