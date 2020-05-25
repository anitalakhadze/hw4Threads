import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class BufferTest {
    List<Transaction> transactions;
    List<Transaction> transactionsLarge;
    static final String PUT = "put";
    static final String TAKE = "take";

    @Before
    public void setUp(){
        transactions = IntStream.range(0, 128)
                .mapToObj(i -> new Transaction(i, i, i))
                .collect(Collectors.toList());
        transactionsLarge = IntStream.range(0, 10000)
                .mapToObj(i -> new Transaction(i, i, i))
                .collect(Collectors.toList());
    }

    @Test
    public void testFillingBufferAndThenSleep() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> logs = new ArrayList<>();
        Buffer buffer = new Buffer(1);
        Thread producer = new Thread(() -> {
            // worker tries to fill the buffer completely.
            // It doesn't get blocked.
            for (int i = 0; i < transactions.size(); i++) {
                try {
                    buffer.add(transactions.get(i));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logs.add(PUT);
            }
            latch.countDown();
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < transactions.size(); i++) {
                try {
                    buffer.remove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logs.add(TAKE);
            }
            latch.countDown();
        });

        producer.start();
        consumer.start();

        latch.await();
        assertEquals(logs.size() / 2, logs.stream().filter(s -> s.equals(PUT)).count());
    }

    @Test
    public void testSmallBufferManyTransactions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> logs = new ArrayList<>();
        Buffer buffer = new Buffer(5);
        Thread producer = new Thread(() -> {
            // worker tries to fill the buffer completely.
            // It doesn't get blocked.
            for (int i = 0; i < transactionsLarge.size(); i++) {
                try {
                    buffer.add(transactionsLarge.get(i));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logs.add(PUT);
            }
            latch.countDown();
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < transactionsLarge.size(); i++) {
                try {
                    buffer.remove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logs.add(TAKE);
            }
            latch.countDown();
        });

        producer.start();
        consumer.start();

        latch.await();
        assertEquals(logs.size() / 2, logs.stream().filter(s -> s.equals(PUT)).count());
    }

}