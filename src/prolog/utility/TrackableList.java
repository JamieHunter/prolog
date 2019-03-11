// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.utility;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Java Lists intentionally hide linkage implementation. However we need to expose this for clause management.
 * List interface is not implemented to prevent being burdened by the abstraction, which is not needed here.
 */
public class TrackableList<E> implements Iterable<E> {

    private final LinkNode<E> root = new LinkNode<>(null);

    /**
     * Creates an array of nodes to allow isolation from node manipulation of underlying list
     *
     * @return Snapshot as an array
     */
    public LinkNode<E>[] snapshot() {
        int size = count();
        @SuppressWarnings("unchecked")
        LinkNode<E>[] snapshot = new LinkNode[size];
        int i = 0;
        for (LinkNode<E> iter = root.next(); iter != root; iter = iter.next()) {
            snapshot[i++] = iter;
        }
        return snapshot;
    }

    /**
     * Retrieve an array of elements.
     * @param a Either array to fill, or a template to use to create a new array from
     * @return array filled with elements
     */
    @SuppressWarnings("unchecked")
    public E[] elements(E[] a) {
        int size = count();
        if (a.length < size)
            a = (E[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (LinkNode<E> iter = root.next(); iter != root; iter = iter.next()) {
            result[i++] = iter.get();
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    /**
     * Determine number of elements in list. This is O(n) operation, use carefully.
     *
     * @return Size of list.
     */
    public int count() {
        LinkNode<E> iter = root.next();
        int count = 0;
        while (iter != root) {
            count++;
            iter = iter.next();
        }
        return count;
    }

    /**
     * Add item to head of list
     *
     * @param item Node to insert
     */
    @SuppressWarnings("unchecked")
    public void addHead(LinkNode item) {
        item.insertAfter(root);
    }

    /**
     * Add item to tail of list
     *
     * @param item Node to insert
     */
    @SuppressWarnings("unchecked")
    public void addTail(LinkNode item) {
        item.insertBefore(root);
    }

    /**
     * @return true if list is empty
     */
    public boolean isEmpty() {
        return root == root.next();
    }

    /**
     * @return List iterator of values
     */
    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    /**
     * @return List iterator of values
     */
    public ListIterator<E> listIterator() {
        return new IteratorImpl<>(root);
    }

    /**
     * @return List iterator of nodes
     */
    public ListIterator<LinkNode<E>> nodeIterator() {
        return new NodeIteratorImpl<>(root);
    }

    /**
     * Implementation if {@link ListIterator} to iterate values
     * @param <E> Type of value
     */
    private static class IteratorImpl<E> implements ListIterator<E> {

        private final NodeIteratorImpl<E> nodes;

        private IteratorImpl(LinkNode<E> root) {
            nodes = new NodeIteratorImpl<>(root);
        }

        @Override
        public boolean hasNext() {
            return nodes.hasNext();
        }

        @Override
        public E next() {
            return nodes.next().get();
        }

        @Override
        public boolean hasPrevious() {
            return nodes.hasPrevious();
        }

        @Override
        public E previous() {
            return nodes.previous().get();
        }

        @Override
        public int nextIndex() {
            return nodes.nextIndex();
        }

        @Override
        public int previousIndex() {
            return nodes.previousIndex();
        }

        @Override
        public void remove() {
            nodes.remove();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("set not implemented");
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("add not implemented");
        }
    }

    /**
     * Implementation of {@link ListIterator} to iterate list nodes.
     * @param <E> Type of value
     */
    private static class NodeIteratorImpl<E> implements ListIterator<LinkNode<E>> {

        private final LinkNode<E> root;
        private LinkNode<E> cursor;
        private LinkNode<E> next;
        private int nextIndex;

        private NodeIteratorImpl(LinkNode<E> root) {
            this.root = root;
            this.cursor = root;
            this.next = root.next();
            this.nextIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return next != root;
        }

        @Override
        public LinkNode<E> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            cursor = next;
            next = next.next();
            nextIndex++;
            return cursor;
        }

        @Override
        public boolean hasPrevious() {
            return next.prev() != root;
        }

        @Override
        public LinkNode<E> previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            cursor = next = next.prev();
            nextIndex--;
            return cursor;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            if (cursor == root) {
                throw new NoSuchElementException();
            }
            cursor.remove();
            if (next == cursor) {
                next = root;
            }
            nextIndex--;
        }

        @Override
        public void set(LinkNode<E> e) {
            throw new UnsupportedOperationException("set not implemented");
        }

        @Override
        public void add(LinkNode<E> e) {
            throw new UnsupportedOperationException("add not implemented");
        }
    }
}
