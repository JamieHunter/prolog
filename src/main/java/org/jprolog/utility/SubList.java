// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Read-only sublist of original list
 *
 * @param <E> Type of element
 */
public class SubList<E> implements List<E>, RandomAccess {
    private final ArrayList<E> parent;
    private final int offset;

    private SubList(ArrayList<E> parent,
                    int offset) {
        this.parent = parent;
        this.offset = offset;
    }

    @SuppressWarnings("unchecked")
    public static <Q> SubList<Q> wrap(List<? extends Q> list) {
        if (list instanceof SubList) {
            return (SubList<Q>) list;
        } else if (list instanceof ArrayList) {
            return new SubList<>((ArrayList<Q>) list, 0);
        } else {
            ArrayList<Q> ar = new ArrayList<>(list);
            return new SubList<>(ar, 0);
        }
    }

    public List<E> subList(int offset) {
        return subList(offset, size());
    }

    public E set(int index, E e) {
        return parent.set(index + offset, e);
    }

    public E get(int index) {
        return parent.get(index + offset);
    }

    public int size() {
        return parent.size() - offset;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    public void add(int index, E e) {
        throw new UnsupportedOperationException("Unsupported");
    }

    public E remove(int index) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("Unsupported");
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Unsupported");
    }

    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        return parent.subList(offset, parent.size()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return parent.subList(offset, parent.size()).toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return parent.subList(offset, parent.size()).containsAll(c);
    }

    public ListIterator<E> listIterator(final int index) {
        final int lower = SubList.this.offset;
        final int upper = parent.size();

        return new ListIterator<E>() {
            int cursor = index + lower;
            int lastRet = -1;

            public boolean hasNext() {
                return cursor != upper;
            }

            public E next() {
                int i = cursor;
                if (i >= upper) {
                    throw new NoSuchElementException();
                }
                cursor = i + 1;
                return parent.get(lastRet = i);
            }

            public boolean hasPrevious() {
                return cursor > lower;
            }

            public E previous() {
                int i = cursor - 1;
                if (i < lower)
                    throw new NoSuchElementException();
                cursor = i;
                return parent.get(lastRet = i);
            }

            public void forEachRemaining(Consumer<? super E> consumer) {
                Objects.requireNonNull(consumer);
                int i = cursor;
                if (i >= upper) {
                    return;
                }
                while (i != upper) {
                    consumer.accept(parent.get(i++));
                }
                // update once at end of iteration to reduce heap write traffic
                lastRet = cursor = i;
            }

            public int nextIndex() {
                return cursor;
            }

            public int previousIndex() {
                return cursor - 1;
            }

            public void remove() {
                throw new UnsupportedOperationException("Unsupported");
            }

            public void set(E e) {
                if (lastRet < 0) {
                    throw new IllegalStateException();
                }
                parent.set(lastRet, e);
            }

            public void add(E e) {
                throw new UnsupportedOperationException("Unsupported");
            }
        };
    }

    public List<E> subList(int fromIndex, int toIndex) {
        if (toIndex != size()) {
            throw new UnsupportedOperationException("toIndex expected to be same as size()");
        }
        if (fromIndex < 0 || fromIndex > size()) {
            throw new IllegalArgumentException("fromIndex out of range");
        }
        return new SubList<>(parent, offset + fromIndex);
    }

    public Spliterator<E> spliterator() {
        return parent.subList(offset, parent.size()).spliterator();
    }
}
