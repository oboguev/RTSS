package rtss.data.mortality;

public class MortalityInfo
{
    // Возраст в годах
    public int x;

    // Числа доживающих до возраста х лет (int)
    public double lx;

    // Числа умирающих при переходе от возраста x к возрасту х+1 лет (int)
    public double dx;

    // Вероятность умереть в течение предстоящего года жизни
    public double qx;

    // Вероятность дожить до возраста х+1 лет
    public double px;

    // Числа живущих в возрасте х лет (int)
    public double Lx;

    // Числа прожитых человеколет (int)
    public double Tx;

    // Средняя продолжительность предстоящей жизни
    public double ex;
}