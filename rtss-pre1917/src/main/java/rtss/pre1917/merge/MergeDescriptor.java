package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.List;

public class MergeDescriptor
{
    public final String combined;
    public final String parent;
    public final List<String> children = new ArrayList<>();

    public MergeDescriptor(String combined, String parent, String... children)
    {
        this.combined = combined;
        this.parent = parent;
        for (String child : children)
            this.children.add(child);
    }

    public String[] childrenAsArray()
    {
        return children.toArray(new String[0]);
    }

    public List<String> parentWithChildren()
    {
        List<String> list = new ArrayList<>();
        list.add(parent);
        list.addAll(children);
        return list;
    }

    public String[] parentWithChildrenAsArray()
    {
        return parentWithChildren().toArray(new String[0]);
    }
    
    public static MergeDescriptor find(List<MergeDescriptor> mds, String combined)
    {
        for (MergeDescriptor md : mds)
        {
            if (combined.equals(md.combined))
                return md;
        }
        
        return null;
    }

    public static MergeDescriptor findContaining(List<MergeDescriptor> mds, String what)
    {
        for (MergeDescriptor md : mds)
        {
            List<String> list = md.parentWithChildren();
            if (list.contains(what))
                return md;
        }
        
        return null;
    }
    
    public static String combined2parent(List<MergeDescriptor> mds, String combined)
    {
        for (MergeDescriptor md : mds)
        {
            if (md.combined.equals(combined))
                return md.parent;
        }
        
        return null;
    }

    public static String parent2combined(List<MergeDescriptor> mds, String parent)
    {
        for (MergeDescriptor md : mds)
        {
            if (md.parent != null && md.parent.equals(parent))
                return md.combined;
        }
        
        return null;
    }
}
