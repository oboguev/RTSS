package rtss.pre1917.data;

import rtss.util.Util;

public class Jews
{
    private final CensusCategories cats;

    public Jews(CensusCategories cats)
    {
        this.cats = cats;
    }

    public Double get(String tname)
    {
        CensusCategoryValues cv = cats.get(tname, false);
        if (cv != null)
            return cv.pct_juifs;

        if (tname.equals(Taxon.Астраханская_кочевники))
            return 0.0;

        switch (tname)
        {
        case Taxon.Астраханская_кочевники:
        case "Выборгская":
            return 0.0;

        case "Холмская":
            return 15.3;

        case Taxon.Астраханская_оседлое:
            tname = "Астраханская";
            break;

        case "г. Баку":
            tname = "Бакинская с Баку";
            break;

        case "г. Николаев":
            tname = "Херсонская с Одессой";
            break;

        case "г. Севастополь":
            tname = "Таврическая с Севастополем";
            break;

        case "Ростовское и./Д град.":
            tname = "Екатеринославская";
            break;
        }

        cv = cats.get(tname);
        if (cv == null)
        {
            Util.err("Jews: no data for " + tname);
            return null;
        }

        return cv.pct_juifs;
    }
}
