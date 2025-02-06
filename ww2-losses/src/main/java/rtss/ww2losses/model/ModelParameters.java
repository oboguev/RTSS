package rtss.ww2losses.model;

import rtss.data.selectors.Area;
import rtss.ww2losses.ageline.warmodel.WarAttritionModelParameters;

public class ModelParameters
{
    public Area area;
    public WarAttritionModelParameters wamp = new WarAttritionModelParameters();
    public boolean PrintDiagnostics;
    public String exportDirectory;

    public ModelParameters()
    {
    }

    public ModelParameters(ModelParameters params)
    {
        this.area = params.area;
        this.wamp = params.wamp.clone();
        this.PrintDiagnostics = params.PrintDiagnostics;
        this.exportDirectory = params.exportDirectory;
    }

    public String toString()
    {
        return String.format("%.3f %.3f %s",
                             wamp.aw_conscript_combat,
                             wamp.aw_civil_combat,
                             area == null ? "none" : area.name());
    }
}
