package data.mortality;

public class MortalityInfo
{
    // Возраст в годах
    public int x;

    // Числа доживающих до возраста х лет
    public int lх;

    // Числа умирающих при переходе от возраста x к возрасту х+1 лет
    public int dх;

    // Вероятность умереть в течение предстоящего года жизни
    public double qх;

    // Вероятность дожить до возраста х+1 лет
    public double px;

    // Числа живущих в возрасте х лет
    public int Lх;

    // Числа прожитых человеколет
    public int Tх;

    // Средняя продолжительность предстоящей жизни
    public double eх;
}
