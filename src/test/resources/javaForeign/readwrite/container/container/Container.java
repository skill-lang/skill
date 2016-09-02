package container;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;

public class Container {


    public Container() {
        maps = new HashMap<>();
        list = new LinkedList<>();
        map = new HashMap<>();
        set = new HashSet<>();
    }

    public LinkedList<Stupid> list;
    public HashSet<String> set;
    public HashMap<Integer, Stupid> map;
    public HashMap<Integer, HashMap<Integer, Integer>> maps;
}
