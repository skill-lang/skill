package container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MyStupidList<E> implements List<E> {

	private ArrayList<E> backing;

	public MyStupidList() {
		backing = new ArrayList<>();
	}

	@Override
	public boolean add(E arg0) {
		return backing.add(arg0);
	}

	@Override
	public void add(int arg0, E arg1) {
		backing.add(arg0, arg1);
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		return backing.addAll(arg0);
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends E> arg1) {
		return backing.addAll(arg0, arg1);
	}

	@Override
	public void clear() {
		backing.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return backing.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return backing.containsAll(arg0);
	}

	@Override
	public E get(int arg0) {
		return backing.get(arg0);
	}

	@Override
	public int indexOf(Object arg0) {
		return backing.indexOf(arg0);
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return backing.iterator();
	}

	@Override
	public int lastIndexOf(Object arg0) {
		return backing.lastIndexOf(arg0);
	}

	@Override
	public ListIterator<E> listIterator() {
		return backing.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int arg0) {
		return backing.listIterator(arg0);
	}

	@Override
	public boolean remove(Object arg0) {
		return backing.remove(arg0);
	}

	@Override
	public E remove(int arg0) {
		return backing.remove(arg0);
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		return backing.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return backing.retainAll(arg0);
	}

	@Override
	public E set(int arg0, E arg1) {
		return backing.set(arg0, arg1);
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public List<E> subList(int arg0, int arg1) {
		return backing.subList(arg0, arg1);
	}

	@Override
	public Object[] toArray() {
		return backing.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return backing.toArray(arg0);
	}

}
