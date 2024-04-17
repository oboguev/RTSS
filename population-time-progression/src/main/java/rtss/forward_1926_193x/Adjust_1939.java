package rtss.forward_1926_193x;

import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;

/*
 * При обработке переписи 1939 года были совершены приписки.
 * 
 * Действительная численность населения СССР составляла 167.6-167.7 млн. чел
 * (а преднамеренная фальсификация около 2,8-2,9 млн., или 1,7%),
 * при этом городское население составляло 47.5 млн. (28.3%), а не 55.9.
 
 * Население РСФСР составляло 106.9 млн. чел., 
 * а городское население 34.1 (31.9%), а не 36.8. 
 * 
 */
public class Adjust_1939
{
    public PopulationByLocality adjust(Area area, final PopulationByLocality p) throws Exception
    {
        // ###
        return p;
    }
}
