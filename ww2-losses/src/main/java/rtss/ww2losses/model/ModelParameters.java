package rtss.ww2losses.model;

import rtss.data.selectors.Area;

public class ModelParameters
{
    public Area area;
    public double aw_conscript_combat;
    public double aw_civil_combat;
    public boolean PrintDiagnostics;
    public String exportDirectory;

    public ModelParameters()
    {
    }

    public ModelParameters(ModelParameters params)
    {
        this.area = params.area;
        this.aw_conscript_combat = params.aw_conscript_combat;
        this.aw_civil_combat = params.aw_civil_combat;
        this.PrintDiagnostics = params.PrintDiagnostics;
        this.exportDirectory = params.exportDirectory;
    }

    public String toString()
    {
        return String.format("%.3f %.3f %s",
                             aw_conscript_combat,
                             aw_civil_combat,
                             area == null ? "none" : area.name());
    }
}
