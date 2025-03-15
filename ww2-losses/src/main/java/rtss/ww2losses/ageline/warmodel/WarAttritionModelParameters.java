package rtss.ww2losses.ageline.warmodel;

/* 
 * Весовые коэффциенты для факторов распределяющих потери по категориям
 */
public class WarAttritionModelParameters
{
    /*
     * Доля потерь мужчин призывного возраста связанная с интенсивностью военных потерь РККА
     */
    public Double aw_conscript_combat = 0.8;

    /*
     * Доля потерь остальных групп (гражданского населения) связанная с интенсивностью военных потерь РККА
     */
    public Double aw_civil_combat = 0.2;
    
    /*
     * Относительная иненсивность потерь в оккупации
     */
    public Double aw_loss_intensity_occupaion = 1.60;

    public WarAttritionModelParameters aw_conscript_combat(double v)
    {
        aw_conscript_combat = v;
        return this;
    }

    public WarAttritionModelParameters aw_civil_combat(double v)
    {
        aw_civil_combat = v;
        return this;
    }

    public WarAttritionModelParameters aw_loss_intensity_occupaion(double v)
    {
        aw_loss_intensity_occupaion = v;
        return this;
    }
    
    public WarAttritionModelParameters clone()
    {
        WarAttritionModelParameters x = new WarAttritionModelParameters();
        x.aw_conscript_combat = aw_conscript_combat;
        x.aw_civil_combat = aw_civil_combat;
        x.aw_loss_intensity_occupaion = aw_loss_intensity_occupaion;
        return x;
    }

    public boolean equals(WarAttritionModelParameters wamp)
    {
        return aw_conscript_combat.equals(wamp.aw_conscript_combat) &&
               aw_civil_combat.equals(wamp.aw_civil_combat) &&
               aw_loss_intensity_occupaion.equals(wamp.aw_loss_intensity_occupaion);
    }
}
