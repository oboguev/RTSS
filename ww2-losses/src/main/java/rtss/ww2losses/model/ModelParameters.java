package rtss.ww2losses.model;

import rtss.data.selectors.Area;

public class ModelParameters
{
    public Area area;
    public double aw_conscripts_rkka_loss;
    public double aw_general_occupation;
    public boolean PrintDiagnostics;
    
    public ModelParameters()
    {
    }
    public ModelParameters(ModelParameters params)
    {
        this.area = params.area;
        this.aw_conscripts_rkka_loss = params.aw_conscripts_rkka_loss;
        this.aw_general_occupation = params.aw_general_occupation;
        this.PrintDiagnostics = params.PrintDiagnostics;
    }
    
    public String toString()
    {
        return String.format("%.3f %.3f %s", 
                             aw_conscripts_rkka_loss, 
                             aw_general_occupation,
                             area == null ? "none" : area.name());
    }
}
