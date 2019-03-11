// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.utility;

/**
 * A node to support linked-lists. See {@link TrackableList} why this is implemented.
 */
public class LinkNode<E> {

    private final E value;
    private LinkNode<E> prev;
    private LinkNode<E> next;

    /**
     * Construct a list node containing a value.
     *
     * @param value Value contained in node.
     */
    public LinkNode(E value) {
        this.value = value;
        prev = next = this;
    }

    /**
     * @return value
     */
    public E get() {
        return value;
    }

    /**
     * @return next node
     */
    public LinkNode<E> next() {
        return next;
    }

    /**
     * @return previous node
     */
    public LinkNode<E> prev() {
        return prev;
    }

    /**
     * @return true if node is removed
     */
    public boolean isRemoved() {
        return next() == this;
    }

    /**
     * Remove node from linked list.
     */
    public void remove() {
        if (!isRemoved()) {
            LinkNode<E> p = prev;
            LinkNode<E> n = next;
            n.prev = p;
            p.next = n;
            prev = next = this;
        }
    }

    /**
     * Merge two lists together by picking two nodes, assumed to be in different lists
     *
     * @param other Other node (will be inserted after this node)
     */
    public void merge(LinkNode<E> other) {
        // This list: A B C D t // assume t at end
        // Other list: o P Q R S // assume o at start
        // New list: A B C D t o P Q R S
        LinkNode<E> end = other.prev; // S in above example
        LinkNode<E> start = this.next; // A in above example
        // link this<->other
        other.prev = this;
        this.next = other;
        // link start/end
        end.next = start;
        start.prev = end;
    }

    /**
     * Split list into two given two reference points.
     *
     * @param node1 First node
     * @param node2 Second node
     */
    public void split(LinkNode<E> node1, LinkNode<E> node2) {
        // Given list A B C D t o P Q R S
        // node1 is t
        // node2 is S
        // create
        // A B C D t and o P Q R S
        LinkNode<E> node1b = node1.next(); // o
        LinkNode<E> node2b = node2.next(); // A
        node1.next = node2b; // t<->A
        node2b.prev = node1;
        node1b.prev = node2; // o<->S
        node2.next = node1b;
    }

    /**
     * Insert this node before other node.
     *
     * @param other Node to insert this node before.
     */
    public void insertBefore(LinkNode<E> other) {
        remove();
        prev = other.prev;
        next = other;
        prev.next = this;
        next.prev = this;
    }

    /**
     * Insert this node after other node.
     *
     * @param other Node to insert this node after.
     */
    public void insertAfter(LinkNode<E> other) {
        remove();
        prev = other;
        next = other.next;
        prev.next = this;
        next.prev = this;
    }
}
