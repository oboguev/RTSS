package data.mortality;

public class MortalityInfo
{
    // Возраст в годах
    public int x;

    // Числа доживающих до возраста х лет
    public int lx;

    // Числа умирающих при переходе от возраста x к возрасту х+1 лет
    public int dx;

    // Вероятность умереть в течение предстоящего года жизни
    public double qx;

    // Вероятность дожить до возраста х+1 лет
    public double px;

    // Числа живущих в возрасте х лет
    public int Lx;

    // Числа прожитых человеколет
    public int Tx;

    // Средняя продолжительность предстоящей жизни
    public double ex;
}
