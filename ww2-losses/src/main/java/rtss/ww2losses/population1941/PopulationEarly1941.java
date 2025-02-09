package rtss.ww2losses.population1941;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.ww2losses.helpers.PopulationContextCache;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population194x.AdjustPopulation;
import rtss.ww2losses.population194x.UtilBase_194x;

/* 
 * Население на начало 1941 года.
 * 
 * К населению на начало 1941 года прилагается крупнозернистая коррекция раскладки численности внутри 5-летних групп.
 * созданной автоматической дезагрегацией 5-летних групп в 1-годовые значения. 
 * Разбивка по 5-летним группам не меняется, но значения для некоторых возрастов перераспределяются 
 * по годам внутри групп так, чтобы избежать артефакта отрицательной величины потерь в 1941-1945 гг.
 */
public class PopulationEarly1941 extends UtilBase_194x
{
    private final AreaParameters ap;
    
    public PopulationEarly1941(AreaParameters ap)
    {
        this.ap = ap;
    }
    
    public PopulationContext evaluate(final AdjustPopulation adjuster1941) throws Exception
    {
        if (useADH)
        {
            String name = "early-1941";
            if (adjuster1941 != null)
                name += "-adjuster-" + adjuster1941.name();
            
            PopulationContext pc = PopulationContextCache.get(ap.area, name, () -> {
                PopulationByLocality p = PopulationADH.getPopulationByLocality(ap.area, 1941);
                if (adjuster1941 != null)
                    p = adjuster1941.adjust(p);
                return p.toPopulationContext();
            });

            return pc;
        }
        else
        {
            /*
             * Убрал код прередвижки от переписи 1939 года и прибавления зап. Украины и зап. Белоруссии
             */
            return null;
        }
    }
}
