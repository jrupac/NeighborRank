import java.util.TreeSet;

/**
 * Why does Java not provide this??
 *
 * Credit goes to: http://stackoverflow.com/questions/7878026/is-there-a-priorityqueue-implementation-with-fixed-capacity-and-custom-comparato
 */
public class FixedSizePriorityQueue<E extends Comparable<E>> extends TreeSet<E> {

    private int maxSize;

    public FixedSizePriorityQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @return true if element was added, false otherwise
     * */
    @Override
    public boolean add(E e) {
        if (maxSize == 0 && size() == 0) {
            // max size was initiated to zero => just return false
            return false;
        } else if (size() < maxSize) {
            // queue isn't full => add element
            return super.add(e);
        } else {
            // there is already 1 or more elements => compare to the least
            if (e.compareTo(this.first()) > 0) {
                // new element is greater than the smallest in queue => pull the smallest and add new one to queue
                pollFirst();
                super.add(e);
                return true;
            } else {
                // new element is less than the smallest in queue => return false
                return false;
            }
        }
    }
}
