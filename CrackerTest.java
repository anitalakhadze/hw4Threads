import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class CrackerTest {
    Cracker c;

    @Before
    public void setUp(){
        c = new Cracker();
    }

    @Test
    public void testHashFunction() throws NoSuchAlgorithmException {
        String expected = "34800e15707fae815d7c90d49de44aca97e2d759";
        String expected2 = "66b27417d37e024c46526c2f6d358a754fc552f3";
        byte[] result = c.generateHash("a!");
        byte[] result2 = c.generateHash("xyz");
        assertEquals(expected, Cracker.hexToString(result));
        assertEquals(expected2, Cracker.hexToString(result2));
    }

//    @Test
//    public void testBaseConversion(){
//        assertEquals("g", c.baseConversion(6, c.CHARS));
//    }

    @Test
    public void testPermutations(){
        c.makeKLengthPerms(2);
        System.out.println(c.allPermutations);
        System.out.println(c.allPermutations.size());
    }


}
