package rtss.mexico.agri.calc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.mexico.agri.entities.ArgiConstants;
import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureDefinition;
import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.SoyaFodder;
import rtss.mexico.agri.loader.CultureDefinitions;
import rtss.mexico.agri.loader.LoadCultureDefinitions;
import rtss.mexico.agri.loader.LoadSoyaFodder;
import rtss.util.Util;

public class Preprocess
{
    private final CultureDefinitions cds = LoadCultureDefinitions.load();
    private final SoyaFodder soyaFodder =LoadSoyaFodder.load(); 
    private final CultureSet cs;

    public Preprocess(CultureSet cs) throws Exception
    {
        this.cs = cs;
    }

    public void preprocess() throws Exception
    {
        /*
         * Данные об экспорте и импорте ванили (Vainilla Beneficiada) за 1925-1982 гг. настолько искажены, 
         * что мы решили исключить этот продукт из исчисляемого набора.
         */
        cs.remove(cs.get("Vainilla Beneficiada"));

        /*
         * Вычислить потребление (там, где оно ещё не вычислено)
         */
        for (Culture c : cs.cultures())
        {
            for (CultureYear cy : c.cultureYears())
            {
                if (cy.consumption == null)
                    cy.consumption = cy.production + denull(cy.importAmount) - denull(cy.exportAmount);
            }
        }

        /* ========================================================================================== */

        /*
         * Согласно набору SARH, в 1971-1982 гг. в среднем 82% оставющихся после экспорта урожаев бараньего гороха (Garbanzo) 
         * шло на фуражные цели, хотя эта величина сильно изменялась год от года между 55% и полным экспортом. 
         * Мы предположим, что и в предшествующие годы та же доля (82%) остатков от экспорта шла на фураж, 
         * оставляя на пищевое потребление 18% урожая бараньего гороха остающегося после экспорта. 
         */
        Culture c1 = cs.get("Garbanzo Grano");
        Culture c2 = cs.get("Garbanzo Para Consumo Humano");
        for (int year : c1.years())
        {
            if (c2.cultureYear(year) != null)
                break;
            CultureYear cy = c2.dupYear(c1.cultureYear(year));
            double fodder = cy.consumption * 0.82;
            cy.consumption -= fodder;
            cy.perCapita = null;
        }

        // оставить только Garbanzo Para Consumo Humano
        cs.remove(c1);

        /* ========================================================================================== */

        // remove cultures with no year data
        for (Culture c : cs.cultures())
        {
            if (c.years().size() == 0)
                cs.remove(c);
        }

        // remove cultures with no calories (фуражные и стимуляторы -- кофе)
        // кроме сахарного тростника
        String sSugarCane = cds.get("sugar cane").name;
        for (Culture c : cs.cultures())
        {
            CultureDefinition cd = cds.get(c.name);
            if (c.name.equals(sSugarCane))
                continue;
            if (cd.kcal_kg == null || cd.kcal_kg == 0)
                cs.remove(c);
        }

        /* ========================================================================================== */

        /*
         * Сахарный тростник употреблялся для производства сахара и алкоголя. 
         * В 1928-1938 гг. средний весовой выход сахара был 6.70% от урожая тростника (с годовыми колебаниями от 6.0 до 7.2%), 
         * а алкоголя 0.57% от урожая тростника (колебания от 0.40 до 0.77%). 
         * Мы прилагаем эти переводные коэфициенты для более ранних лет, в которые производство сахара и алкоголя 
         * не отражено непосредственными сведениями, и имеются только сведения об урожае тростника.
         */
        Culture cSugar = cs.get("sugar");
        Culture cSugarCane = cs.get("sugar cane");

        for (CultureYear cy : cSugarCane.cultureYears())
        {
            if (cy.alcohol != null)
                break;
            cy.alcohol = cy.production * ArgiConstants.SugarCaneToAlcohol;
        }

        for (CultureYear cy : cSugarCane.cultureYears())
        {
            if (cSugar.cultureYear(cy.year) != null)
                break;
            CultureYear cySugar = cSugar.makeCultureYear(cy.year);
            cySugar.consumption = cySugar.production = cy.production * ArgiConstants.SugarCaneToSugar;
        }

        /* ========================================================================================== */

        /*
         * Приближение данных о внешней тороговле для периода 1897-1908 гг. для лет и позиций,
         * по которым не имеется сведений 
         */
        approximateEarlyExport("jitomate", 45.0);
        approximateEarlyExport("frijol", 2.5);
        approximateEarlyExport("chile verde", 12.0);
        approximateEarlyExport("arroz", 8.0);

        approximateEarlyImport("trigo", 12.0);
        approximateEarlyImport("maiz", 0.8);

        /* ========================================================================================== */

        /*
         * Сорго практически не употребляется как пищевая культура 
         */
        cs.remove(cs.get("Sorgo Grano"));

        /*
         * Вычесть траты на семена, фураж и потери. 
         */
        applyReductions();

        /* ========================================================================================== */

        /*
         * Для некоторых лет в период 1925-1982 объём экспорта второстепенных культур изредка превосходит 
         * объём производства культуры в текущий год, т.к. экспортируются остатки урожая предыдущего года. 
         * Это вызывает отрицательную величину потребления культуры в данный год. Для лет с отрицательными 
         * значениями эффективного потребления мы устанавливаем величину потребления в этот год равной нулю 
         * и распределяем отрицательный баланс на предыдущие годы.
         */
        eliminateConsumptionNegatives();

        Util.noop();
    }

    private void approximateEarlyExport(String cname, double pct) throws Exception
    {
        Culture c = cs.get(cname);

        for (CultureYear cy : c.cultureYears())
        {
            if (cy.year >= 1909)
                break;
            if (cy.exportAmount != null || cy.importAmount != null)
                continue;
            double amount = cy.production * pct / 100.0;
            cy.exportAmount = amount;
            cy.consumption -= amount;
            cy.perCapita = null;
        }
    }

    private void approximateEarlyImport(String cname, double pct) throws Exception
    {
        Culture c = cs.get(cname);

        for (CultureYear cy : c.cultureYears())
        {
            if (cy.year >= 1909)
                break;
            if (cy.exportAmount != null || cy.importAmount != null)
                continue;
            double amount = cy.production * pct / 100.0;
            cy.importAmount = amount;
            cy.consumption += amount;
            cy.perCapita = null;
        }
    }

    private double denull(Double d)
    {
        return d == null ? 0 : d;
    }

    private void eliminateConsumptionNegatives() throws Exception
    {
        List<CultureYear> negatives = new ArrayList<>();

        for (Culture c : cs.cultures())
        {
            List<CultureYear> cys = c.cultureYears();
            Collections.reverse(cys);
            for (CultureYear cy : cys)
            {
                if (cy.consumption < 0)
                {
                    // Util.out(String.format("Negative consumption %s %d", cy.culture.id(), cy.year));
                    negatives.add(cy);
                }
            }
        }

        if (negatives.size() != 0)
        {
            for (CultureYear cy : negatives)
                eliminateConsumptionNegatives(cy);
            // eliminateConsumptionNegatives();
        }

    }

    private void eliminateConsumptionNegatives(CultureYear cy) throws Exception
    {
        while (cy.consumption < 0)
        {
            CultureYear pcy = cy.culture.cultureYear(cy.year - 1);
            pcy.consumption += cy.consumption;
            cy.consumption = 0.0;
            cy = pcy;
        }
    }

    private void applyReductions() throws Exception
    {
        for (Culture c : cs.cultures())
        {
            CultureDefinition cd = cds.get(c.name);
            applyReduction(c, cd.seed_pct, cd.fodder_pct, ArgiConstants.LossPercentage);
        }
    }

    private void applyReduction(Culture c, Double... pcts) throws Exception
    {
        boolean soya = cds.getRequired("soya").name.equals(c.name);

        double pct = 0;

        for (Double p : pcts)
        {
            if (p != null)
                pct += p;
        }

        if (pct > 0 || soya)
        {
            for (CultureYear cy : c.cultureYears())
            {
                double xpct = pct;
                if (soya)
                    xpct += soyaFodder.pct(cy.year);

                cy.consumption *= (100.0 - xpct) / 100.0;
            }
        }
    }
}
