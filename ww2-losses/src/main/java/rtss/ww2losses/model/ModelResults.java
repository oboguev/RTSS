package rtss.ww2losses.model;

public class ModelResults
{
    /* сумма избыточных смертей (с.изб) с середины 1941 по конец 1945 гг. */
    public double actual_excess_wartime_deaths;
    
    /* сумма избыточных смертей мужчин призывного возраста (с.прз) с середины 1941 по конец 1945 гг. */
    public double exd_conscripts;
    
    /* сумма избыточных смертей среди родившихся после середины 1941 года (с.инов) по конец 1945 гг. */
    public double excess_warborn_deaths;
    
    /* фактическое число рождений (р.факт) с середины 1941 по конец 1945 гг. */
    public double actual_births;

    /* смертность в 1942 году */
    public double cdr_1942;

    /* сумма иммиграции с середины 1941 по конец 1945 гг., только для РСФСР */
    public double immigration;
}
