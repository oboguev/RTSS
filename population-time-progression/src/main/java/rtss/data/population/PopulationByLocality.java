package rtss.data.population;

import rtss.data.ValueConstraint;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Holds populations by type of locality: rural, urban, total (rural + urban)
 */
public class PopulationByLocality
{
    public static final int MAX_AGE = Population.MAX_AGE;

    private Population rural;
    private Population urban;
    private Population total;

    private PopulationByLocality()
    {
    }

    public PopulationByLocality(Population total) throws Exception
    {
        this.total = total;
        validate();
    }

    public PopulationByLocality(Population total, Population urban, Population rural) throws Exception
    {
        this.total = total;
        this.urban = urban;
        this.rural = rural;
        validate();
    }

    public PopulationByLocality(Population urban, Population rural) throws Exception
    {
        this.urban = urban;
        this.rural = rural;
        recalcTotalLocalityFromUrbanRural();
        validate();
    }

    static public PopulationByLocality newPopulationByLocality()
    {
        PopulationByLocality p = new PopulationByLocality();
        p.rural = Population.newPopulation(Locality.RURAL);
        p.urban = Population.newPopulation(Locality.URBAN);
        p.total = Population.newPopulation(Locality.TOTAL);
        return p;
    }

    static public PopulationByLocality newPopulationTotalOnly()
    {
        PopulationByLocality p = new PopulationByLocality();
        p.total = Population.newPopulation(Locality.TOTAL);
        return p;
    }

    public PopulationByLocality clone()
    {
        PopulationByLocality p = new PopulationByLocality();

        if (rural != null)
            p.rural = rural.clone();
        if (urban != null)
            p.urban = urban.clone();
        if (total != null)
            p.total = total.clone();

        return p;
    }

    public PopulationByLocality cloneTotalOnly()
    {
        PopulationByLocality p = new PopulationByLocality();

        if (total != null)
            p.total = total.clone();

        return p;
    }

    public boolean hasRuralUrban()
    {
        return rural != null && urban != null;
    }

    public boolean hasTotal()
    {
        return total != null;
    }

    /****************************************************************************************************/

    public double get(Locality locality, Gender gender, int age) throws Exception
    {
        return forLocality(locality).get(gender, age);
    }

    public double sum(Locality locality, Gender gender, int age1, int age2) throws Exception
    {
        return forLocality(locality).sum(gender, age1, age2);
    }

    public void set(Locality locality, Gender gender, int age, double value) throws Exception
    {
        forLocality(locality).set(gender, age, value);
    }

    public void add(Locality locality, Gender gender, int age, double value) throws Exception
    {
        forLocality(locality).add(gender, age, value);
    }

    public void sub(Locality locality, Gender gender, int age, double value) throws Exception
    {
        forLocality(locality).sub(gender, age, value);
    }

    public Population forLocality(Locality locality)
    {
        switch (locality)
        {
        case RURAL:
            return rural;
        case URBAN:
            return urban;
        case TOTAL:
            return total;
        default:
            return null;
        }
    }

    public void resetUnknownForEveryLocality() throws Exception
    {
        if (rural != null)
            rural.resetUnknown();
        if (urban != null)
            urban.resetUnknown();
        if (total != null)
            total.resetUnknown();
    }

    public void recalcTotalForEveryLocality() throws Exception
    {
        if (rural != null)
            rural.recalcTotal();
        if (urban != null)
            urban.recalcTotal();
        if (total != null)
            total.recalcTotal();
    }

    public void makeBoth(Locality locality) throws Exception
    {
        forLocality(locality).makeBoth();
    }

    public void makeBoth() throws Exception
    {
        if (rural != null)
            rural.makeBoth();
        if (urban != null)
            urban.makeBoth();
        if (total != null)
            total.makeBoth();
    }

    public void recalcFromUrbanRuralBasic() throws Exception
    {
        makeBoth(Locality.RURAL);
        makeBoth(Locality.URBAN);
        recalcTotalLocalityFromUrbanRural();
    }

    /*
     * Выборка [age1 ... age2] или [ageday1 ... ageday2] .
     * 
     * Нецелое значение года означает, что население выбирается только от/до этой возрастной точки.
     * Так age2 = 80.0 означает, что население с возраста 80.0 лет исключено. 
     * Аналогично, age2 = 80.5 означает, что включена половина населения в возрасте 80 лет,
     * а население начиная с возраста 81 года исключено целиком. 
     */
    public PopulationByLocality selectByAge(double age1, double age2) throws Exception
    {
        if (rural != null && urban != null)
        {
            Population x_urban = urban.selectByAge(age1, age2);
            Population x_rural = rural.selectByAge(age1, age2);
            return new PopulationByLocality(x_urban, x_rural);
        }
        else
        {
            Population x_total = total.selectByAge(age1, age2);
            return new PopulationByLocality(x_total);
        }
    }

    public PopulationByLocality selectByAge(int ageday1, int ageday2) throws Exception
    {
        if (rural != null && urban != null)
        {
            Population x_urban = urban.selectByAge(ageday1, ageday2);
            Population x_rural = rural.selectByAge(ageday1, ageday2);
            return new PopulationByLocality(x_urban, x_rural);
        }
        else
        {
            Population x_total = total.selectByAge(ageday1, ageday2);
            return new PopulationByLocality(x_total);
        }
    }
    /*
     * Вернуть результат вычитания @this - @p
     */
    public PopulationByLocality sub(PopulationByLocality p) throws Exception
    {
        return sub(p, null);
    }
    
    public PopulationByLocality sub(PopulationByLocality p, ValueConstraint rvc) throws Exception
    {
        if ((rural != null) != (p.rural != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((urban != null) != (p.urban != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((total != null) != (p.total != null))
            throw new IllegalArgumentException("населения разнотипны");

        PopulationByLocality res = new PopulationByLocality();

        if (rural != null)
            res.rural = rural.sub(p.rural, rvc);

        if (urban != null)
            res.urban = urban.sub(p.urban, rvc);

        if (total != null)
            res.total = total.sub(p.total, rvc);

        return res;
    }

    /*
     * Вернуть результат вычитания @this + @p
     */
    public PopulationByLocality add(PopulationByLocality p) throws Exception
    {
        return add(p, null);
    }
    
    public PopulationByLocality add(PopulationByLocality p, ValueConstraint rvc) throws Exception
    {
        if ((rural != null) != (p.rural != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((urban != null) != (p.urban != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((total != null) != (p.total != null))
            throw new IllegalArgumentException("населения разнотипны");

        PopulationByLocality res = new PopulationByLocality();

        if (rural != null)
            res.rural = rural.add(p.rural, rvc);

        if (urban != null)
            res.urban = urban.add(p.urban, rvc);

        if (total != null)
            res.total = total.add(p.total, rvc);

        return res;
    }

    /*
     * Вернуть результат (@this + @p) / 2
     */
    public PopulationByLocality avg(PopulationByLocality p) throws Exception
    {
        return avg(p, null);
    }
    
    public PopulationByLocality avg(PopulationByLocality p, ValueConstraint rvc) throws Exception
    {
        PopulationByLocality p2 = this.add(p, rvc);
        p2 = RescalePopulation.scaleAllBy(p2, 0.5);
        return p2;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вверх
     */
    public PopulationByLocality moveUp(double years) throws Exception
    {
        PopulationByLocality res = new PopulationByLocality();

        if (rural != null)
            res.rural = rural.moveUp(years);

        if (urban != null)
            res.urban = urban.moveUp(years);

        if (total != null)
            res.total = total.moveUp(years);

        return res;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вниз
     */
    public PopulationByLocality moveDown(double years) throws Exception
    {
        PopulationByLocality res = new PopulationByLocality();

        if (rural != null)
            res.rural = rural.moveDown(years);

        if (urban != null)
            res.urban = urban.moveDown(years);

        if (total != null)
            res.total = total.moveDown(years);

        return res;
    }

    /****************************************************************************************************/

    public static PopulationByLocality census(Area area, int year) throws Exception
    {
        return load(String.format("population_data/%s/%d", area.name(), year));
    }

    public static PopulationByLocality load(String path) throws Exception
    {
        PopulationByLocality p = new PopulationByLocality();
        p.do_load(path);
        return p;
    }

    private void do_load(String path) throws Exception
    {
        rural = load(path, Locality.RURAL);
        urban = load(path, Locality.URBAN);
        if (haveFile(path, Locality.TOTAL))
        {
            total = load(path, Locality.TOTAL);
        }
        else
        {
            total = calcTotal(rural, urban);
        }

        validate();
    }

    public void saveToFiles(String dirPath, String comment) throws Exception
    {
        if (rural != null)
            rural.saveToFile(dirPath, comment);

        if (urban != null)
            urban.saveToFile(dirPath, comment);

        if (total != null)
            total.saveToFile(dirPath, comment);
    }

    public void recalcTotalLocalityFromUrbanRural() throws Exception
    {
        total = calcTotal(rural, urban);
    }

    private Population load(String path, Locality locality) throws Exception
    {
        return Population.load(filePath(path, locality), locality);
    }

    private String filePath(String path, Locality locality)
    {
        return String.format("%s/%s.txt", path, locality.toString());
    }

    private boolean haveFile(String path, Locality locality)
    {
        return null != Util.class.getClassLoader().getResource(filePath(path, locality));
    }

    public void validate() throws Exception
    {
        if (rural != null)
        {
            if (rural.locality != Locality.RURAL)
                mismatch();
            rural.validate();
        }

        if (urban != null)
        {
            if (urban.locality != Locality.URBAN)
                mismatch();
            urban.validate();
        }

        if (total != null)
        {
            if (total.locality != Locality.TOTAL)
                mismatch();
            total.validate();
        }

        if (rural != null && urban != null)
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                if (Util.differ(rural.male(age) + urban.male(age), total.male(age)))
                    mismatch();

                if (Util.differ(rural.female(age) + urban.female(age), total.female(age)))
                    mismatch();

                if (Util.differ(rural.fm(age) + urban.fm(age), total.fm(age)))
                    mismatch();
            }
        }
    }

    static private Population calcTotal(Population rural, Population urban) throws Exception
    {
        Population total = Population.newPopulation(Locality.TOTAL);

        for (int age = 0; age <= MAX_AGE; age++)
        {
            total.male.put(age, rural.male.get(age) + urban.male.get(age));
            total.female.put(age, rural.female.get(age) + urban.female.get(age));
            total.both.put(age, rural.both.get(age) + urban.both.get(age));
        }

        total.male_total = rural.male_total + urban.male_total;
        total.female_total = rural.female_total + urban.female_total;
        total.both_total = rural.both_total + urban.both_total;

        total.male_unknown = rural.male_unknown + urban.male_unknown;
        total.female_unknown = rural.female_unknown + urban.female_unknown;
        total.both_unknown = rural.both_unknown + urban.both_unknown;

        total.validate();

        return total;
    }

    /****************************************************************************************************/

    private void mismatch() throws Exception
    {
        throw new Exception("Mismatching data in population table");
    }

    /****************************************************************************************************/

    public PopulationByLocality smooth(boolean doSmooth) throws Exception
    {
        PopulationByLocality p = clone();

        if (doSmooth)
        {
            if (p.rural != null && p.urban != null)
            {
                p.rural.smooth();
                p.urban.smooth();
                p.recalcTotalLocalityFromUrbanRural();
            }
            else
            {
                p.total.smooth();
            }

            validate();
        }

        return p;
    }

    public double[] toArray(Locality locality, Gender gender) throws Exception
    {
        return forLocality(locality).toArray(gender);
    }

    /****************************************************************************************************/

    @Override
    public String toString()
    {
        try
        {
            StringBuilder sb = new StringBuilder();

            if (total != null)
                sb.append(total.toString("total."));

            if (urban != null)
            {
                if (sb.length() != 0)
                    sb.append(" ");
                sb.append(urban.toString("urban."));
            }

            if (rural != null)
            {
                if (sb.length() != 0)
                    sb.append(" ");
                sb.append(rural.toString("rural."));
            }

            return sb.toString();
        }
        catch (Throwable ex)
        {
            return "<exception while formating>";
        }
    }

    public static final String STRUCT_014 = Population.STRUCT_014;
    public static final String STRUCT_0459 = Population.STRUCT_0459;

    public String ageStructure014() throws Exception
    {
        return ageStructure(STRUCT_014, Locality.TOTAL, Gender.BOTH);
    }

    public String ageStructure0459() throws Exception
    {
        return ageStructure(STRUCT_0459, Locality.TOTAL, Gender.BOTH);
    }

    public String ageStructure(String struct) throws Exception
    {
        return ageStructure(struct, Locality.TOTAL, Gender.BOTH);
    }

    public String ageStructure(String struct, String whichLocality, String whichGender) throws Exception
    {
        Locality locality;
        Gender gender;

        switch (whichLocality.trim().toLowerCase())
        {
        case "total":
            locality = Locality.TOTAL;
            break;

        case "rural":
            locality = Locality.RURAL;
            break;

        case "urban":
            locality = Locality.URBAN;
            break;

        default:
            throw new Exception("Invalid locality selector: " + whichLocality);
        }

        switch (whichGender.trim().toLowerCase())
        {
        case "mf":
        case "fm":
        case "both":
            gender = Gender.BOTH;
            break;

        case "m":
        case "male":
            gender = Gender.MALE;
            break;

        case "f":
        case "female":
            gender = Gender.FEMALE;
            break;

        default:
            throw new Exception("Invalid gender selector: " + whichGender);
        }

        return ageStructure(struct, locality, gender);
    }

    public String ageStructure(String struct, Locality locality, Gender gender) throws Exception
    {
        switch (locality)
        {
        case TOTAL:
            return total.ageStructure(struct, gender);

        case URBAN:
            return urban.ageStructure(struct, gender);

        case RURAL:
            return rural.ageStructure(struct, gender);

        default:
            throw new IllegalArgumentException();
        }
    }

    public void setValueConstraint(ValueConstraint vc)
    {
        if (rural != null)
            rural.setValueConstraint(vc);

        if (urban != null)
            urban.setValueConstraint(vc);

        if (total != null)
            total.setValueConstraint(vc);
    }

    public String dump() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        
        if (total != null)
        {
            sb.append("TOTAL:");
            sb.append(Util.nl);
            sb.append(total.dump());
            sb.append("");
        }
 
        if (urban != null)
        {
            sb.append("URBAN:");
            sb.append(Util.nl);
            sb.append(urban.dump());
            sb.append("");
        }

        if (rural != null)
        {
            sb.append("RURAL:");
            sb.append(Util.nl);
            sb.append(rural.dump());
            sb.append("");
        }
        
        return sb.toString();
    }

    public void validateBMF() throws Exception
    {
        if (total != null)
            total.validateBMF();
 
        if (urban != null)
            urban.validateBMF();

        if (rural != null)
            rural.validateBMF();
    }
    
    public void zero() throws Exception
    {
        if (total != null)
            total.zero();
 
        if (urban != null)
            urban.zero();

        if (rural != null)
            rural.zero();
    }
}
