package rtss.mexico.agri.entities;

public class ArgiConstants
{
    /*
     * Переводной коэфициент для выхода чищенного риса из риса сырца.
     */
    public static final double RawRiceToWhiteRice = 0.66;

    /*
     * Переводной коэфициент для выхода сахара из сахарного тростника.
     */
    public static final double SugarCaneToSugar = 6.60 * 0.01;

    /*
     * Переводной коэфициент для выхода алкоголя из сахарного тростника (литров на тонну).
     */
    public static final double SugarCaneToAlcohol = 0.57 * 0.01;
    
    /*
     * Калорий в литре чистого алкоголя.
     */
    public static final double CaloriesPerLiterOfAlcohol = 5600;
    
    /*
     * Процент потерь.
     */
    public static final double LossPercentage = 5.0;
}
