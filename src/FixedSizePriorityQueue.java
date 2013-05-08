import java.util.TreeSet;

/**
 * Why does Java not provide this??
 *
 * Credit goes to: http://stackoverflow.com/questions/7878026/is-there-a-priorityqueue-implementation-with-fixed-capacity-and-custom-comparato
 */
public class FixedSizePriorityQueue<E extends Comparable<E>> extends TreeSet<E> {

    private int elementsLeft;

    public FixedSizePriorityQueue(int maxSize) {
        this.elementsLeft = maxSize;
    }

    /**
     * @return true if element was added, false otherwise
     * */
    @Override
    public boolean add(E e) {
        if (elementsLeft == 0 && size() == 0) {
            // max size was initiated to zero => just return false
            return false;
        } else if (elementsLeft > 0) {
            // queue isn't full => add element and decrement elementsLeft
            boolean added = super.add(e);
            if (added) {
                elementsLeft--;
            }
            return added;
        } else {
            // there is already 1 or more elements => compare to the least
            boolean less = e.compareTo(this.last()) < 0;
            if (less) {
                // new element is less than the largest in queue => pull the largest and add new one to queue
                pollLast();
                super.add(e);
                return true;
            } else {
                // new element is greater than the largest in queue => return false
                return false;
            }
        }
    }
}
