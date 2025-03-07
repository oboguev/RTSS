package rtss.pre1917.tools;

import java.util.HashSet;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.AdjustTerritories;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.util.Util;

public class ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Прогрессивные величины исчисляются отсчётом от переписи 1897 года с прибавлением ежегодного");
            Util.out("числа рождений минус смертей (по УГВИ) и миграционного баланса области (по ЦСК).");
            Util.out("");
            Util.out("Недорегистрация рождений и смертей ведёт к искажению прогрессивной численности населения,");
            Util.out("вероятнее всего (но не обязательно) в сторону его недоучёта и занижения.");
            Util.out("");
            Util.out("В миграции учитыватся только крестьянское переселение, но не движение рабочих,");
            Util.out("поэтому миграционная оценка для Бакинского района или Донбасса занижена,");
            Util.out("а в местах выхода рабочих соответственно не учтена.");
            Util.out("");
            Util.out("Прогрессивная оценка не вычисляется для");
            Util.out("    Камчатской области (создана в 1909), Батумской области (создана в 1903), Холмской губернии (создана в 1912)");
            Util.out("т.к. для промежутка между 1897 годом и моментом их создания сведения о естественом движении");
            Util.out("не включены в базу (хотя могут быть добавлены позднее, из уездных сведений УГВИ).");
            Util.out("");
            Util.out("Оценка по стабилизированному участку вычисляется на основе лет, в которые была достигнута");
            Util.out("удовлетворительная полнота регистрации рождений и смертей, в предположении, что в остальные годы");
            Util.out("рождаемость и смертность была такой же, как в среднем по стабилизированному участку.");
            Util.out("Годы стабилизированного участка помечены звёздочкой cправа.");
            Util.out("");
            new ShowAreaValues().show_values_select();
            new ShowAreaValues().show_values_central_asia();
            new ShowAreaValues().show_values_cacauses();
            new ShowAreaValues().show_values_fixed();
            new ShowAreaValues(LoadOptions.MERGE_POST1897_REGIONS).show_values_post1897();
            new ShowAreaValues().show_values_southern();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    protected final TerritoryDataSet tdsUGVI;
    protected final TerritoryDataSet tdsCSK;
    protected final TerritoryDataSet tdsCensus1897;
    protected final TotalMigration totalMigration;
    protected final EvalGrowthRate evalGrowthRate;

    private boolean onlyRaw = false;

    private final char NBSP = 0xA0;
    private final String NBSP_S = "" + NBSP;

    public ShowAreaValues(TerritoryDataSet tdsUGVI,
            TerritoryDataSet tdsCSK,
            TerritoryDataSet tdsCensus1897) throws Exception
    {
        this.tdsUGVI = tdsUGVI;
        this.tdsCSK = tdsCSK;
        this.tdsCensus1897 = tdsCensus1897;
        totalMigration = TotalMigration.getTotalMigration();
        evalGrowthRate = new EvalGrowthRate(tdsCensus1897);
    }

    public void setOnlyRaw()
    {
        onlyRaw = true;
    }

    public ShowAreaValues(LoadOptions... options) throws Exception
    {
        Set<LoadOptions> xo = Set.of(options);

        tdsUGVI = new LoadData().loadUGVI(unite(xo,
                                                LoadOptions.DONT_VERIFY,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.EVAL_PROGRESSIVE,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD));
        tdsCSK = new LoadData().loadEzhegodnikRossii(unite(xo, LoadOptions.DONT_VERIFY));
        tdsCensus1897 = new LoadData().loadCensus1897(unite(xo,
                                                            LoadOptions.DONT_VERIFY,
                                                            LoadOptions.MERGE_CITIES));
        totalMigration = TotalMigration.getTotalMigration();
        evalGrowthRate = new EvalGrowthRate(tdsCensus1897);
    }

    public ShowAreaValues() throws Exception
    {
        this(new LoadOptions[0]);
    }

    private Set<LoadOptions> unite(Set<LoadOptions> xo, LoadOptions... options)
    {
        xo = new HashSet<>(xo);
        for (LoadOptions opt : options)
            xo.add(opt);
        return xo;
    }

    /* ============================================================================================== */

    @SuppressWarnings("unused")
    private void show_values_select() throws Exception
    {
        Util.out("");
        Util.out("===================================== РАЗНЫЕ ОБЛАСТИ ===================================== ");
        Util.out("");

        show_values("Уральская обл.");
        show_values("Терская обл.");
    }

    @SuppressWarnings("unused")
    private void show_values_central_asia() throws Exception
    {
        Util.out("");
        Util.out("===================================== СРЕДНЯЯ АЗИЯ ===================================== ");
        Util.out("");

        show_values("Акмолинская обл.");
        show_values("Закаспийская обл.");
        show_values("Самаркандская обл.");
        show_values("Семипалатинская обл.");
        show_values("Семиреченская обл.");
        show_values("Сыр-Дарьинская обл.");
        show_values("Тургайская обл.");
        show_values("Уральская обл.");
        show_values("Ферганская обл.");
    }

    @SuppressWarnings("unused")
    private void show_values_cacauses() throws Exception
    {
        Util.out("");
        Util.out("===================================== КАВКАЗ ===================================== ");
        Util.out("");

        show_values("г. Баку");
        show_values("Бакинская");
        show_values("Бакинская с Баку");
        show_values("Батумская");
        show_values("Дагестанская обл.");
        show_values("Елисаветпольская");
        show_values("Карсская обл.");
        show_values("Кубанская обл.");
        show_values("Кутаисская");
        show_values("Ставропольская");
        show_values("Терская обл.");
        show_values("Тифлисская");
        show_values("Черноморская");
        show_values("Эриванская");
        show_values("Закатальский окр.");
        show_values("Сухумский окр.");
    }

    @SuppressWarnings("unused")
    private void show_values_fixed() throws Exception
    {
        Util.out("");
        Util.out("===================================== СКОРРЕКТИРОВАННЫЕ ЗНАЧЕНИЯ ===================================== ");
        Util.out("");

        new AdjustTerritories(tdsUGVI).fixDagestan();

        show_values("Дагестанская обл.");
    }

    /* ============================================================================================== */

    @SuppressWarnings("unused")
    private void show_values_post1897() throws Exception
    {
        Util.out("");
        Util.out("============= КОМИБИНИРОВАННЫЕ ЗНАЧЕНИЯ С ГУБЕРНИЯМИ ОБЛАСТЯМИ СОЗДАННЫМИ ПОСЛЕ 1897 ГОДА =============");
        Util.out("");

        show_values("Кутаисская с Батумской");
        show_values("Иркутская с Камчатской");
        show_values("Люблинская с Седлецкой и Холмской");
    }

    /* ============================================================================================== */

    @SuppressWarnings("unused")
    private void show_values_southern() throws Exception
    {
        Util.out("");
        Util.out("============= НЕКОТОРЫЕ ЮЖНЫЕ ГУБЕРНИИ =============");
        Util.out("");

        show_values("Воронежская");
        show_values("Полтавская");
        show_values("Харьковская");
        show_values("Астраханская");
        show_values("Курская");
        show_values("Екатеринославская");
    }

    /* ============================================================================================== */

    protected void show_values(String tname) throws Exception
    {
        Territory t = tdsUGVI.get(tname);

        if (t == null)
        {
            Util.out("************************************************************");
            Util.out("");
            Util.out("Нет сведений для " + tname);
            Util.out("");
            return;
        }
        
        show(t, null);
    }

    public void show(Territory t, String prefix) throws Exception
    {
        Territory tCSK = tdsCSK.get(t.name);
        Territory tEval = onlyRaw ? null : evalGrowthRate.evalTerritory(t);
        
        if (prefix == null)
        {
            Util.out("");
            Util.out("************************************************************");
            Util.out("");
            Util.out("Рождаемость и смертность для " + t.name);
            Util.out("");
        }
        else
        {
            Util.out("");
            Util.out(prefix + "Рождаемость и смертность для " + t.name);
            Util.out("");
        }
    
        if (tEval == null)
        {
            Util.out("год       ЦСК             УГВИ                 чр      чс    мигр      прогрессивный от 1897");
            Util.out("==== =========== ========================================== =======  ========================");
        }
        else
        {
            Util.out("год       ЦСК             УГВИ                 чр      чс    мигр      прогрессивный от 1897      по стабилиз. участку     чр      чс    учёт %");
            Util.out("==== =========== ========================================== =======  ========================  ========================================= =======");
        }

        for (int year : t.years())
        {
            if (year >= 1896 && year <= 1914)
            {
                TerritoryYear ty = t.territoryYear(year);
                Double cbrUGVI = rate(ty.births.total.both, ty.population.total.both);
                Double cdrUGVI = rate(ty.deaths.total.both, ty.population.total.both);

                Double cbrProgressive = rate(ty.births.total.both, ty.progressive_population.total.both);
                Double cdrProgressive = rate(ty.deaths.total.both, ty.progressive_population.total.both);

                TerritoryYear tyEval = null;
                Double cbrEval = null;
                Double cdrEval = null;
                Long popEval = null;
                Long birthsEval = null;
                Long deathsEval = null;

                if (tEval != null)
                {
                    tyEval = tEval.territoryYear(year);
                    popEval = tyEval.population.total.both;
                    birthsEval = tyEval.births.total.both;
                    deathsEval = tyEval.deaths.total.both;
                    cbrEval = rate(birthsEval, popEval);
                    cdrEval = rate(deathsEval, popEval);
                }

                TerritoryYear tyCSK = null;
                Long popCSK = null;
                if (tCSK != null)
                    tyCSK = tCSK.territoryYearOrNull(year);
                if (tyCSK != null)
                    popCSK = tyCSK.population.total.both;

                String stable = NBSP_S;
                if (evalGrowthRate.is_stable_year(t.name, year))
                    stable = "*";

                long saldo = totalMigration.saldo(t.name, year);

                String s = String.format("%d %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s",
                                         // год
                                         year,
                                         // ЦСК
                                         s_population(popCSK),
                                         // УГВИ
                                         s_population(ty.population.total.both),
                                         s_rate(cbrUGVI), s_rate(cdrUGVI), s_ep(cbrUGVI, cdrUGVI),
                                         s_bd(ty.births.total.both),
                                         s_bd(ty.deaths.total.both),
                                         // миграционное сальдо
                                         s_saldo(saldo),
                                         // прогрессивный расчёт
                                         s_population(ty.progressive_population.total.both),
                                         s_rate(cbrProgressive), s_rate(cdrProgressive), s_ep(cbrProgressive, cdrProgressive),
                                         // по стаб. участку
                                         s_population(popEval),
                                         s_rate(cbrEval), s_rate(cdrEval), s_ep(cbrEval, cdrEval),
                                         s_bd(birthsEval), s_bd(deathsEval),
                                         stable,
                                         s_pct(ty.births.total.both, birthsEval), s_pct(ty.deaths.total.both, deathsEval));

                Util.out(s.trim());
            }
        }
    }

    private String s_pct(Long a, Long b)
    {
        String s = "";
        if (a != null && b != null)
            s += Math.round((100.0 * a) / b);
        return pad(s, 3);
    }

    private String s_population(Long v)
    {
        String s = "";
        if (v != null)
            s = String.format("%,d", v);
        return pad(s, 11);
    }

    private String s_saldo(Long v)
    {
        String s = "";
        if (v != null)
            s = String.format("%,d", v);
        return pad(s, 7);
    }

    private String s_bd(Long v)
    {
        String s = "";
        if (v != null)
            s = String.format("%,d", v);
        return pad(s, 7);
    }

    private String s_rate(Double rate)
    {
        String s = "";
        if (rate != null)
            s = String.format("%2.1f", rate);
        return pad(s, 4);
    }

    private String s_ep(Double cbr, Double cdr)
    {
        String s = "";
        if (cbr != null && cdr != null)
            s = String.format("%2.1f", cbr - cdr);
        return pad(s, 4);
    }

    private Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * 1000.0) / pop;
    }

    private String pad(String s, int length)
    {
        while (s.length() < length && s.length() == 0)
            s = NBSP_S;

        while (s.length() < length)
            s = " " + s;

        return s;
    }
}
