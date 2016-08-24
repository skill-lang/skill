package container;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;

public class Container {

    public LinkedList<Integer> intlist;

    public HashSet<String> strset;

    public LinkedList<Stupid> stupidlist;
    public HashSet<Stupid> stupidset;
    public HashMap<Stupid, Stupid> stupidmap;

    public LinkedList<Integer> getIntlist() { return intlist; }
    public HashSet<String> getStrset() { return strset; }
    public void setIntlist(LinkedList<Integer> il) { intlist = il; }
    public void setStrset(HashSet<String> ss) { strset = ss; }

}
