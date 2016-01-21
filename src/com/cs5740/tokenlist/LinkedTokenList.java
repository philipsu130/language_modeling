package com.cs5740.tokenlist;

import com.cs5740.models.NgramModel;

/**
 * We did better?
 */
public class LinkedTokenList implements TokenList {
    private static class Node {
        public final String value;
        public Node next;
        public Node(final String value) {
            this.value = value;
        }
    }

    public LinkedTokenList(String... args) {
        for (final String s : args) {
            addLast(s);
        }
    }

    Node head = null;
    // decided against using butt, with great reluctance
    Node tail = null;
    int size = 0;

    @Override
    public void addFirst(String element) {
        if (head == null) {
            head = tail = new Node(element);
        } else {
            final Node newHead = new Node(element);
            newHead.next = head;
            head = newHead;
        }
        size++;
    }

    @Override
    public void addLast(final String element) {
        if (head == null) {
            head = tail = new Node(element);
        } else {
            final Node newTail = new Node(element);
            tail.next = newTail;
            tail = newTail;
        }
        size++;
    }

    @Override
    public String head() {
        return head.value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public TokenList tail() {
        final LinkedTokenList linkedTokenList = new LinkedTokenList();
        linkedTokenList.head = head.next;
        linkedTokenList.tail = tail;
        linkedTokenList.size = size - 1;
        return linkedTokenList;
    }

    @Override
    public boolean containsUnknown() {
        Node curr = head;
        while (curr != null) {
            if (curr.value.equals(NgramModel.UNKNOWN_WORD_TOKEN)) {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("[ ");
        Node curr = head;
        while (curr != null) {
            stringBuilder.append(curr.value).append(" ");
            curr = curr.next;
        }
        return stringBuilder.append("]").toString();
    }
}
