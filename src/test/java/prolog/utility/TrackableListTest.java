package prolog.utility;

import org.junit.Test;


import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TrackableListTest {

    private static class Foo {
    }

    @Test
    public void testCreate() {
        TrackableList<Foo> list = new TrackableList<>();
        assertThat(list.isEmpty(), is(true));
        assertThat(list.count(), is(0));
        LinkNode<Foo> [] nodes = list.snapshot();
        assertThat(nodes, notNullValue());
        assertThat(nodes.length, is(0));
    }

    @Test
    public void testSingleElementList() {
        TrackableList<Foo> list = new TrackableList<>();
        Foo element = new Foo();
        LinkNode<Foo> node = new LinkNode<>(element);
        assertThat(node.isRemoved(), is(true));
        list.addTail(node);
        assertThat(node.isRemoved(), is(false));
        assertThat(list.count(), is(1));
        LinkNode<Foo> [] nodes = list.snapshot();
        assertThat(nodes.length, is(1));
        assertThat(nodes[0], is(node));
        // iterate a single element as nodes
        ListIterator<LinkNode<Foo>> nodeIter = list.nodeIterator();
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(false));
        assertThat(nodeIter.nextIndex(), is(0));
        LinkNode<Foo> iterNode = nodeIter.next();
        assertThat(iterNode, is(node));
        assertThat(nodeIter.nextIndex(), is(1));
        assertThat(nodeIter.hasNext(), is(false));
        assertThat(nodeIter.hasPrevious(), is(true));
        iterNode = nodeIter.previous();
        assertThat(iterNode, is(node));
        assertThat(nodeIter.nextIndex(), is(0));
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(false));
        nodeIter.remove();
        assertThat(node.isRemoved(), is(true));
        assertThat(nodeIter.hasNext(), is(false));
        assertThat(nodeIter.hasPrevious(), is(false));
    }

    @Test
    public void testAddHeadElements() {
        TrackableList<Foo> list = new TrackableList<>();
        Foo el1 = new Foo();
        Foo el2 = new Foo();
        Foo el3 = new Foo();
        LinkNode<Foo> node1 = new LinkNode<>(el1);
        LinkNode<Foo> node2 = new LinkNode<>(el2);
        LinkNode<Foo> node3 = new LinkNode<>(el3);

        list.addHead(node1);
        list.addHead(node2);
        list.addHead(node3);

        assertThat(list.count(), is(3));
        LinkNode<Foo> [] nodes = list.snapshot();
        assertThat(nodes.length, is(3));
        assertThat(nodes[0], is(node3));
        assertThat(nodes[1], is(node2));
        assertThat(nodes[2], is(node1));

        // iterate nodes
        ListIterator<LinkNode<Foo>> nodeIter = list.nodeIterator();
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(false));
        assertThat(nodeIter.nextIndex(), is(0));

        LinkNode<Foo> iterNode = nodeIter.next();
        assertThat(iterNode, is(node3));
        assertThat(nodeIter.nextIndex(), is(1));
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(true));

        iterNode = nodeIter.next();
        assertThat(iterNode, is(node2));
        assertThat(nodeIter.nextIndex(), is(2));
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(true));

        iterNode = nodeIter.next();
        assertThat(iterNode, is(node1));
        assertThat(nodeIter.nextIndex(), is(3));
        assertThat(nodeIter.hasNext(), is(false));
        assertThat(nodeIter.hasPrevious(), is(true));

        iterNode = nodeIter.previous();
        assertThat(iterNode, is(node1));
        assertThat(nodeIter.nextIndex(), is(2));
        assertThat(nodeIter.previousIndex(), is(1));
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(true));

        iterNode = nodeIter.previous();
        assertThat(iterNode, is(node2));
        assertThat(nodeIter.nextIndex(), is(1));
        assertThat(nodeIter.previousIndex(), is(0));
        assertThat(nodeIter.hasNext(), is(true));
        assertThat(nodeIter.hasPrevious(), is(true));

        nodeIter.remove();
        assertThat(node2.isRemoved(), is(true));
        assertThat(node1.isRemoved(), is(false));
        assertThat(node3.isRemoved(), is(false));

        assertThat(list.count(), is(2));
        nodes = list.snapshot();
        assertThat(nodes.length, is(2));
        assertThat(nodes[0], is(node3));
        assertThat(nodes[1], is(node1));
    }

    @Test
    public void testAddTailElements() {
        TrackableList<Foo> list = new TrackableList<>();
        Foo el1 = new Foo();
        Foo el2 = new Foo();
        Foo el3 = new Foo();
        LinkNode<Foo> node1 = new LinkNode<>(el1);
        LinkNode<Foo> node2 = new LinkNode<>(el2);
        LinkNode<Foo> node3 = new LinkNode<>(el3);

        list.addTail(node1);
        list.addTail(node2);
        list.addTail(node3);

        assertThat(list.count(), is(3));
        LinkNode<Foo> [] nodes = list.snapshot();
        assertThat(nodes.length, is(3));
        assertThat(nodes[0], is(node1));
        assertThat(nodes[1], is(node2));
        assertThat(nodes[2], is(node3));

        Foo [] elements = list.elements(new Foo[0]);
        assertThat(elements.length, is(3));
        assertThat(elements[0], is(el1));
        assertThat(elements[1], is(el2));
        assertThat(elements[2], is(el3));

        Foo [] elements2 = new Foo[elements.length+1];
        Foo [] elements3 = list.elements(elements2);
        assertThat(elements3, is(elements2));
        assertThat(elements3[0], is(el1));
        assertThat(elements3[1], is(el2));
        assertThat(elements3[2], is(el3));
        assertThat(elements3[3], nullValue());

        // iterate elements
        ListIterator<Foo> iter = list.listIterator();
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.hasPrevious(), is(false));
        assertThat(iter.nextIndex(), is(0));

        Foo el = iter.next();
        assertThat(el, is(el1));
        assertThat(iter.nextIndex(), is(1));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.hasPrevious(), is(true));

        el = iter.next();
        assertThat(el, is(el2));
        assertThat(iter.nextIndex(), is(2));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.hasPrevious(), is(true));

        iter.remove();
        assertThat(node2.isRemoved(), is(true));
        assertThat(node1.isRemoved(), is(false));
        assertThat(node3.isRemoved(), is(false));
        assertThat(list.count(), is(2));
    }
}
