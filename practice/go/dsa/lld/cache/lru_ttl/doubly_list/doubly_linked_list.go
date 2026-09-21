package doubly_list

import "fmt"

type DoublyLinkedListNode[T comparable, U any] struct {
	Key   T
	Value U
	next  *DoublyLinkedListNode[T, U]
	prev  *DoublyLinkedListNode[T, U]
}

type DoublyLinkedList[T comparable, U any] struct {
	head *DoublyLinkedListNode[T, U]
	tail *DoublyLinkedListNode[T, U]
	Size int
}

func NewDoublyLinkedList[T comparable, U any]() *DoublyLinkedList[T, U] {
	return &DoublyLinkedList[T, U]{
		head: nil,
		tail: nil,
		Size: 0,
	}
}

func (list *DoublyLinkedList[T, U]) AddNodeFirst(node *DoublyLinkedListNode[T, U]) {
	if list.head == nil {
		list.head = node
		list.tail = node
	} else {
		list.head.prev = node
		node.next = list.head
		list.head = node
	}
	list.Size++
}

func (list *DoublyLinkedList[T, U]) AddNodeLast(node *DoublyLinkedListNode[T, U]) {
	if list.head == nil {
		list.head = node
		list.tail = node
	} else {
		list.tail.next = node
		node.prev = list.tail
		list.tail = node
	}
	list.Size++
}

func (list *DoublyLinkedList[T, U]) Remove(node *DoublyLinkedListNode[T, U]) {
	next := node.next
	prev := node.prev

	if next != nil {
		next.prev = prev
	}

	if prev != nil {
		prev.next = next
	}

	if node == list.head {
		list.head = next
	}

	if node == list.tail {
		list.tail = prev
	}

	node.next = nil
	node.prev = nil

	list.Size--
}

func (list *DoublyLinkedList[T, U]) RemoveFirst() (*DoublyLinkedListNode[T, U], error) {
	if list.head == nil {
		return nil, fmt.Errorf("list empty")
	}

	head := list.head
	list.Remove(head)
	return head, nil
}

func (list *DoublyLinkedList[T, U]) RemoveLast() (*DoublyLinkedListNode[T, U], error) {
	if list.tail == nil {
		return nil, fmt.Errorf("list empty")
	}

	tail := list.tail
	list.Remove(tail)
	return tail, nil
}
