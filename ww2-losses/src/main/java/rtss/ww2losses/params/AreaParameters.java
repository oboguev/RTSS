package rtss.ww2losses.params;

import rtss.data.rates.Recalibrate;
import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;

public class AreaParameters
{
    public /*final*/ Area area;

    /*
     * Уровни рождаемости и смертности CBR_xxx и CDR_xxx нормированы на численность населения в начале календарного года.
     * Уровни рождаемости и смертности CBR_xxx_MIDYEAR и CDR_xxx_MIDYEAR нормированы на среднегодовую численность населения.
     * 
     * Для исчисления будущего прироста от численности населения в текущей точке следует использовать CBR_xxx и CDR_xxx,
     * т.к. они выражают градиенты dP(t)/dt в текущей точке P(t=0) по монотонному восходящему направа отрезку P(t=[0,T]).
     * 
     * CBR_xxx_MIDYEAR и CDR_xxx_MIDYEAR отражают исчисление относительно P(t=0) по промежутку [-T,T] состоящему из двух
     * монотонных отрезков: восходящему направо P(t=[0,T]) и нисходящему налево P(t=[0,-T]). Приложение параметров
     * CBR_xxx_MIDYEAR и CDR_xxx_MIDYEAR к численности населения P(t=0) охватывает не только возрастание наседения на участке
     * t=[0,T], но и его уменьшение на участке t=[-T,0]. 
     * 
     * Соответственно, CBR_xxx и CDR_xxx применимы для "глядящего вперёд" вычисления от точки t=0 вычисления числа рождений и смертей,
     * по отрезку распространяющемуся вперёд во времени от момента t=0.
     *     
     * CBR_xxx_MIDYEAR и CDR_xxx_MIDYEAR приложимы для вычисления числа рождений и смертей по отрезку расползающемуся от момента
     * t=0 в обеих направлениях: [-T,T].
     * 
     * В данных исчислениях CBR_xxx_MIDYEAR и CDR_xxx_MIDYEAR относимы к t=0 соответствующему середине календарного года. 
     * CBR_xxx и CDR_xxx вычисляются для t=0 соответствующему началу календарного года.
     * 
     */

    /* birth and death rates in 1939 */
    public double CBR_1939;
    public double CDR_1939;
    public double CBR_1939_MIDYEAR;
    public double CDR_1939_MIDYEAR;

    /* birth and death rates in 1940 */
    public double CBR_1940;
    public double CDR_1940;
    public double CBR_1940_MIDYEAR;
    public double CDR_1940_MIDYEAR;

    /* 
     * прирост населения в 1940 году, с учётом миграции, 
     * если не задан, то полагается равным естественному приросту (CBR_1940 - CDR_1940) 
     * */
    public final /*final*/ Double growth_1940 = null;

    /* birth and death rates in 1946 */
    public double CBR_1946;
    public double CDR_1946;
    public double CBR_1946_MIDYEAR;
    public double CDR_1946_MIDYEAR;

    /* население по АДХ на начало 1939 и начало 1940 гг. */
    public double ADH_MALES_1939;
    public double ADH_FEMALES_1939;
    public double ADH_MALES_1940;
    public double ADH_FEMALES_1940;

    /* ======================================================================= */

    protected AreaParameters(Area area)
    {
        this.area = area;
    }

    static public AreaParameters forArea(Area area) throws Exception
    {
        switch (area)
        {
        case RSFSR:
            return new AreaParameters_RSFSR();
        case USSR:
            return new AreaParameters_USSR();
        default:
            throw new IllegalArgumentException();
        }
    }

    public double growth_1940()
    {
        if (growth_1940 != null)
            return growth_1940;
        else
            return CBR_1940 - CDR_1940;
    }

    public void build() throws Exception
    {
        /*
         * Перекалибровать на численность населения в начале года
         */
        if (area == Area.USSR)
        {
            Rates r = Recalibrate.m2e(new Rates(CBR_1939_MIDYEAR, CDR_1939_MIDYEAR));
            CBR_1939 = r.cbr;
            CDR_1939 = r.cdr;
        }
        else
        {
            CBR_1939 = Recalibrate.m2e(this.area, 1939, CBR_1939_MIDYEAR);
            CDR_1939 = Recalibrate.m2e(this.area, 1939, CDR_1939_MIDYEAR);
        }

        CBR_1940 = Recalibrate.m2e(this.area, 1940, CBR_1940_MIDYEAR);
        CDR_1940 = Recalibrate.m2e(this.area, 1940, CDR_1940_MIDYEAR);

        CBR_1946 = Recalibrate.m2e(this.area, 1946, CBR_1946_MIDYEAR);
        CDR_1946 = Recalibrate.m2e(this.area, 1946, CDR_1946_MIDYEAR);
    }
}
