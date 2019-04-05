/* Copyright (c) 2015-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package expressivo;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the Expression abstract data type.
 */
public class ExpressionTest {
	
    // Flags for additional equality tests
	private static final int IGNORE_WS = 0x1;
    private static final int TEST_HC   = 0x2;
    private static final int ALL_FLAGS = IGNORE_WS & TEST_HC;
	
	@Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }
    
    @Test
    public void testWhitespaceDoesntMatter() {
    	assertEquals("Whitespace shouldn't matter", "     hello      ", "hello", IGNORE_WS);
    	assertEquals("Whitespace shouldn't matter", "  H  e ll o \t", "\t\t\tHello", IGNORE_WS);
    }

	/*
	 * Testing strategy:
	 * - Binop & Primitive:
	 *    - an object ported to string and back should be equal to itself
	 * - Binop:
	 *    - check they work with Variables, Numbers, and one of each
	 *    - check combinations of each other work
	 * - Structural equality:
	 *    - Primitive(a) == Primitive(a)
	 *    - Binop(a, b) == Binop(a, b)
	 *    - Binop(a, b) != Binop(b, a)
	 *    - TODO Binop(a, Binop(b, c)) vs Binop(Binop(a, b), c) ???
	 *    - Number(1) == Number(1.0000)
	 * - Grouping
	 *    - TODO ???
	 * - Hashcode:
	 *    - equals() will work by comparing hashcodes, so the structural
	 *      equality tests shoud cover this
	 */
	
	@Test public void testVariablesAlone() {
		Variable v = new Variable("foo");
		assertEquals("Variable's string representation should be correct", 
				"foo", v.toString(), IGNORE_WS);
		assertEquals("Variables with the same ident should equal each other", 
				new Variable("foo"), new Variable("foo"), TEST_HC);
	}
	
	@Test public void testNumbersAlone() {
		Numeric i = new Numeric("1");
		assertEquals("Int's string representation should be correct",
				i.toString(), "1");
		assertEquals("Strings of ints with the same value should equal each other",
				i.toString(), new Numeric("1").toString());
		
		assertEquals("Identical Numeric ints should equal each other",
				i, new Numeric("1"), ALL_FLAGS);
		
		assertEquals("Float string should be correct", 
				new Numeric("1.05").toString(), "1.05");
		assertEquals("Float string shouldn't have trailing zeroes",
				new Numeric("1.050").toString(), "1.05");
		assertEquals("Strings of floats with the same value should equal each otehr",
				new Numeric("1.05").toString(), new Numeric("1.05").toString());
		assertEquals("Identical Numeric floats should equal each other",
				new Numeric("6.98"), new Numeric("6.98"), ALL_FLAGS);
	}
	
	@SuppressWarnings("unused")
	@Test(expected=IllegalArgumentException.class) 
	public void testNumericErrorConditions() {
		Numeric inval = new Numeric("foo");
	}
	
	@Test public void testNumericEqualityBetweenFloatAndInt() {
		assertEquals("Int's string should equal a whole-number float's string", 
				new Numeric("1").toString(), new Numeric("1.00").toString());
		assertEquals("Int Number should equal a whole-number float Number",
				new Numeric("1"), new Numeric("1.00"));
	}
	
	@Test public void testNumberLimits() {
		assertEquals("Numbers > INT_MAX should work correctly", 
				new Numeric("2147483648").toString(), "2147483648");
		assertNotEquals("Numbers that aren't quite equal shouldn't compare as equal",
				 new Numeric("1.0"), new Numeric("1.00000000000000000000000001")); 
	}
	
	@Test public void testSum() {
		assertEquals("Identical sums should equal each other",
				new Sum(new Numeric("1"), new Variable("foo")),
				new Sum(new Numeric("1"), new Variable("foo")), ALL_FLAGS);
		assertEquals("Identical sums' string representations should equal each other",
				new Sum(new Variable("foo"), new Variable("bar")).toString(),
				new Sum(new Variable("foo"), new Variable("bar")).toString(), ALL_FLAGS);
		Sum s1 = new Sum(new Numeric("1"), new Numeric("2"));
		Sum s2 = new Sum(new Numeric("3"), new Numeric("4"));
		assertEquals("Sums should commute without arbitrary parentheses", 
				"1+2+3+4", new Sum(s1, s2).toString(), IGNORE_WS);
	}
	
	@Test public void testProduct() {
		assertEquals("Identical products should equal each other",
				new Product(new Numeric("1"), new Variable("foo")),
				new Product(new Numeric("1"), new Variable("foo")), ALL_FLAGS);
		assertEquals("Identical products' string representations should equal each other",
				new Product(new Variable("foo"), new Variable("bar")).toString(),
				new Product(new Variable("foo"), new Variable("bar")).toString(), IGNORE_WS);
		Product p1 = new Product(new Numeric("1"), new Numeric("2"));
		Product p2 = new Product(new Numeric("3"), new Numeric("4"));
		assertEquals("Products should commute without arbitrary parentheses",
				"1*2*3*4", new Product(p1, p2).toString(), IGNORE_WS);
	}
	
	@Test public void testBinopStructuralEquality() {
		Variable a = new Variable("a");
		Variable b = new Variable("b");
		assertNotEquals("Sums with swapped lvalue/rvalue should not compare equal",
				new Sum(a, b), new Sum(b, a));
		assertNotEquals("Sums with swapped lvalue/rvalue should not hash equal",
				new Sum(a, b).hashCode(), new Sum(b, a).hashCode());
		assertNotEquals("Products with swapped lvalue/rvalue should not compare equal",
				new Product(a, b), new Product(b, a)); 
		assertNotEquals("Products with swapped lvalue/rvalue should not hash equal",
				new Product(a, b).hashCode(), new Product(b, a).hashCode()); 
	}
	
	@Test public void testExpressionAddsParenthesesWhenNeeded() {
		Variable a = new Variable("a");
		Expression multFirst = new Sum(new Product(a,a), new Numeric("1"));
		Expression addFirst = new Product(new Sum(a, new Numeric("1")), a);
				
		assertEquals("Shouldn't add parens unless necessary", "a*a+1", multFirst.toString(), IGNORE_WS);
		assertEquals("Should add parens to keep BIDMAS order", "(a+1)*a", addFirst.toString(), IGNORE_WS);
		assertFalse("Non-commutative expressions in different order shouldn't be equal",
				multFirst.equals(addFirst));
		
		Variable b = new Variable("b");
		Expression aabb = new Sum(new Sum(a,a), new Sum(b,b)); 
		assertEquals("Should keep BIDMAS order (longer sum)", 
				"(a+a+b+b)*2", new Product(aabb, new Numeric("2")).toString());
		
		// These also check for any aliasing bugs by passing in the same object twice
		assertEquals("Shouldn't add parens unless necessary in complex subexpressions",
				"(a+1)*a*(a+1)*a", new Product(addFirst, addFirst).toString(), IGNORE_WS);
		assertEquals("Should add parens to keep BIDMAS in complex subexpressions", 
				"(a*a+1)*(a*a+1)", new Product(multFirst, multFirst).toString(), IGNORE_WS);

	}
	
	@Test public void testComplexExpressions() {
		Variable w = new Variable("w");
		Variable x = new Variable("x");
		Variable y = new Variable("y");
		Variable z = new Variable("z");
		Numeric five = new Numeric("5");
		Numeric pi = new Numeric("3.1415927");
		
		assertEquals("Complex expression 1", 
				"(x+y)*5*(x+3.1415927)", 
				new Product(
						new Product(new Sum(x,y),five), 
						new Sum(x, pi)
					).toString());
		
		assertEquals("Complex expression 2",
				"(x+y)*(5*(x+y)+3.1415927*(x+5))",
				new Product(
						new Sum(x,y), 
						new Sum(
								new Product(five, new Sum(x,y)),
								new Product(pi, new Sum(x,five))
						)
					).toString());
		
		Sum xplusy = new Sum(x, y);
		Sum yplusx = new Sum(y, x);
		Sum zplusw = new Sum(z, w);
		Sum wplusz = new Sum(w, z);
		Sum xplusyplusz = new Sum(new Sum(x, y), z);
		
		assertEquals("Complex expression 3",
				"((x+y)*(x+y)+(z+w)*(z+w))*((w+z)*(w+z)+(y+x)*(y+x))",
				new Product(new Sum(pow(xplusy, 2), pow(zplusw, 2)),
							new Sum(pow(wplusz, 2), pow(yplusx, 2))).toString());
		
		assertEquals("Complex expression 4",
				"(x+y+z)*(x+y+z)*(x+y+z)",
				pow(xplusyplusz, 3).toString());
	}
	
	Expression pow(Expression base, int exp) {
		Expression ret = base;
	    for (int i = 1; i < exp; i++) {
			ret = new Product(ret, base);
		}
	    return ret;
	}
	
	/*
	 * Whitespace does not matter in tests here, so ignore it in string
	 * equality tests
	 */
	void assertEquals(String msg, Object expected, Object actual, int flags) {
		if (expected instanceof String 
				&& actual instanceof String
				&& (flags & IGNORE_WS) != 0) {
			String o1 = ((String) expected).replaceAll("\\s", "");
			String o2 = ((String) actual).replaceAll("\\s", "");
			Assert.assertEquals(msg, o1, o2);
		} else {
			Assert.assertEquals(msg, expected, actual);
		}
		if ((flags & TEST_HC) != 0) {
			Assert.assertEquals(msg, expected.hashCode(), actual.hashCode());
		}
	}
	
	void assertEquals(String msg, Object expected, Object actual) {
		Assert.assertEquals(msg, expected, actual);
	}
	
}
