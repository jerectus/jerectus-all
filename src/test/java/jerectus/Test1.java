package jerectus;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import org.junit.Test;

public class Test1 {
    public static class QueueIterator<T> implements Iterator<T> {
        private Consumer<Queue<T>> consumer;
        private Queue<T> cache = new LinkedList<>();

        public QueueIterator(Consumer<Queue<T>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public boolean hasNext() {
            if (cache.isEmpty()) {
                consumer.accept(cache);
            }
            return !cache.isEmpty();
        }

        @Override
        public T next() {
            return hasNext() ? cache.poll() : null;
        }
    }

    @Test
    public void test1() {

    }
}