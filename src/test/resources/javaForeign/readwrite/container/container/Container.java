package container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;

public class Container {


    public Container() {
        maps = new LinkedHashMap<>();
        list = new ArrayList<>();
        map = new TreeMap<>();
        set = new TreeSet<>();
    }

    public ArrayList<Stupid> list;
    public TreeSet<String> set;
    public TreeMap<Integer, Stupid> map;
    public HashMap<Integer, HashMap<Integer, Integer>> maps;
}
