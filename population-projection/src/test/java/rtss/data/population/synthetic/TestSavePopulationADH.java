package rtss.data.population.synthetic;

import java.io.File;

import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptionsByGender;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

/*
 * Сохранить население по АДХ в файлах
 */
public class TestSavePopulationADH
{
    public static void main(String[] args)
    {
        try
        {
            new TestSavePopulationADH().main_USSR();
            new TestSavePopulationADH().main_RSFSR();

            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    private void main_USSR() throws Exception
    {
        Area area = Area.USSR;
        main(area, 1926);
        main(area, 1927);
        main(area, 1937);
        main(area, 1938);
        main(area, "1939-границы-1938");
        main(area, "1939-границы-1946");
        main(area, 1940);
        main(area, 1941);
        main(area, 1946);
        main(area, 1947);
    }

    private void main_RSFSR() throws Exception
    {
        Area area = Area.RSFSR;
        
        for (int year = 1927; year <= 1959; year++)
        {
            if (year >= 1942 && year <= 1945)
                continue;
            main(area, year);
        }
    }
    
    private void main(Area area, int year) throws Exception
    {
        main(area, "" + year);
    }

    private void main(Area area, String year) throws Exception
    {
        InterpolationOptionsByGender options = new InterpolationOptionsByGender().allowCache(false);
        options.both().secondaryRefineYearlyAgesSmoothness(0.50);

        PopulationByLocality p = PopulationADH.getPopulationByLocality(area, year, options);
        
        if (Util.True)
        {
            File rootDir = new File("c:\\@@capture\\ADH-population-" + area);
            String en_year = year.replace("-границы-", "-in-borders-of-");
            File dir = new File(rootDir, "" + en_year);
            dir.mkdirs();
            String comment = String.format("Таблица построена дезагрегацией данных АДХ при помощи модуля %s", 
                                           PopulationADH.class.getCanonicalName());
            p.saveToFiles(dir.getAbsoluteFile().getCanonicalPath(), comment);
        }
    }
}
