/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for reported issue between JEXL-200 and JEXL-299.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IssuesTest200 extends JexlTestCase {
    public IssuesTest200() {
        super("IssuesTest200", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    public static class Eval {
        private JexlEngine jexl;

        public JexlScript fn(String src) {
            return jexl.createScript(src);
        }

        void setJexl(JexlEngine je) {
            jexl = je;
        }
    }

    @Test
    public void test200() throws Exception {
        JexlContext jc = new MapContext();
        Map<String, Object> funcs = new HashMap<String, Object>();
        Eval eval = new Eval();
        funcs.put(null, eval);
        JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
        eval.setJexl(jexl);
        String src = "var f = fn(\'(x)->{x + 42}\'); f(y)";
        JexlScript s200 = jexl.createScript(src, "y");
        Assert.assertEquals(142, s200.execute(jc, 100));
        Assert.assertEquals(52, s200.execute(jc, 10));
    }

    @Test
    public void test200b() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = 0; var f = (y)->{ x = y; }; f(42); x");
        Object r = e.execute(jc);
        Assert.assertEquals(0, r);
    }

    @Test
    public void test209a() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x.a = ()->{return 1}; x['a']()");
        Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    @Test
    public void test209b() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x['a'] = ()->{return 1}; x.a()");
        Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    public class T210 {
        public void npe() {
            throw new NullPointerException("NPE210");
        }
    }

    @Test
    public void test210() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("v210", new T210());
        JexlEngine jexl = new JexlBuilder().strict(false).silent(false).create();
        JexlScript e = jexl.createScript("v210.npe()");
        try {
            e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(JexlException xjexl) {
            Throwable th = xjexl.getCause();
            Assert.assertEquals("NPE210", th.getMessage());
        }
    }

    @Test
    public void test217() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        jc.set("foo", new int[]{0, 1, 2, 42});
        JexlEngine jexl;
        JexlScript e;
        Object r;
        jexl = new JexlBuilder().strict(false).silent(false).create();
        e = jexl.createScript("foo[3]");
        r = e.execute(jc);
        Assert.assertEquals(42, r);

        // cache and fail?
        jc.set("foo", new int[]{0, 1});
        jc.setStrict(true);
        try {
            r = e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(JexlException xjexl) {
            Throwable th = xjexl.getCause();
            Assert.assertTrue(ArrayIndexOutOfBoundsException.class.equals(th.getClass()));
        }
        //
        jc.setStrict(false);
        r = e.execute(jc);
        Assert.assertNull("oob adverted", r);
    }


    @Test
    public void test221() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        jc.set("map", map);
        JexlEngine jexl = new JexlBuilder().cache(256).create();
        JexlScript e = jexl.createScript("(x)->{ map[x] }");
        Object r;
        r = e.execute(jc, (Object) null);
        Assert.assertEquals(null, r);
        r = e.execute(jc, (Object) null);
        Assert.assertEquals(null, r);
        r = e.execute(jc, "one");
        Assert.assertEquals(1, r);
    }


    public static class JexlArithmetic224 extends JexlArithmetic {
        public JexlArithmetic224(boolean astrict) {
            super(astrict);
        }

        protected Object nth(Collection<?> c, int i) {
            if (c instanceof List) {
                // tell engine to use default
                return JexlEngine.TRY_FAILED;
            }
            for (Object o : c) {
                if (i-- == 0) {
                    return o;
                }
            }
            return null;
        }

        public Object propertyGet(Collection<?> c, Number n) {
            return nth(c, n.intValue());
        }

        public Object arrayGet(Collection<?> c, Number n) {
            return nth(c, n.intValue());
        }

        public Object call(Collection<?> c, Number n) {
            if (c instanceof List) {
                return ((List) c).get(n.intValue());
            }
            return nth(c, n.intValue());
        }
    }

    @Test
    public void test224() throws Exception {
        List<String> a0 = Arrays.asList("one", "two");
        Set<String> a1 = new TreeSet<String>(a0);
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic224(true)).create();
        Object r;
        JexlScript e = jexl.createScript("(map, x)->{ map[x] }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map)->{ map.1 }");
        r = e.execute(jc, a0);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map, x)->{ map(x) }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
    }

    public static class Context225 extends MapContext {
        public String bar(){
            return "bar";
        }
    }

    @Test
    public void test225() throws Exception {
        Context225 df = new Context225();
        JexlEngine jexl = new JexlBuilder().create();

        JexlExpression expression = jexl.createExpression("bar()");
        Assert.assertEquals("bar", expression.evaluate(df));
        ObjectContext<Object> context = new ObjectContext<Object>(jexl, df);
        Assert.assertEquals("bar", expression.evaluate(context));
    }
}
