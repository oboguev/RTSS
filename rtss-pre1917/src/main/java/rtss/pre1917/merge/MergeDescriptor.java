package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.List;

public class MergeDescriptor
{
    public final String root;
    public final String parent;
    public final List<String> children = new ArrayList<>();
    
    public MergeDescriptor(String root, String parent, String... children)
    {
        this.root = root;
        this.parent = parent;
        for (String child : children)
            this.children.add(child);
    }
    
    public List<String> parentWithChildren()
    {
        List<String> list = new ArrayList<>();
        list.add(parent);
        list.addAll(children);
        return list;
    }
}
