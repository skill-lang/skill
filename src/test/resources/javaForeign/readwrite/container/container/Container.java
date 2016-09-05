package container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class Container {


    public Container() {
        maps = new LinkedHashMap<>();
        list = new ArrayList<>();
        map = new TreeMap<>();
        set = new HashSet<>();
    }

    public List<Stupid> list;
    public Set<String> set;
    public Map<Integer, Stupid> map;
    public Map<Integer, Map<Integer, Integer>> maps;
}
