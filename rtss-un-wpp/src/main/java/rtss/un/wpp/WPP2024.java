package rtss.un.wpp;

/*
 * To load the WPP file, use Java heap size 16G:
 *     java -Xmx16g
 */
public class WPP2024 extends WPP
{
    public WPP2024() throws Exception
    {
        super("un-wpp-2024/most-used/WPP2024_GEN_F01_DEMOGRAPHIC_INDICATORS_FULL.xlsx", "Estimates");
    }
}
