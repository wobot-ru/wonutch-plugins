package ru.wobot.sm.core.reflect;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodInvoker_Test {

    private TargetSpy testTarget;

    @Before
    public void setUp() {
        testTarget = new TargetSpy();
    }

    @Test
    public void when_invoke_method_with_params() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final String result = new MethodInvoker(testTarget, getTestTargetMethodByName("method1")).invoke("s1", "s2");
        assertThat(testTarget.isMethod1Invoked(), equalTo(true));
        assertThat(result, equalTo("s1s2"));
    }

    @Test
    public void when_invoke_void_method_with_no_params() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        new MethodInvoker(testTarget, getTestTargetMethodByName("method2")).invoke();
        assertThat(testTarget.isMethod2Invoked(), equalTo(true));
    }

    public static Method getTestTargetMethodByName(String name) {
        for (Method method : TargetSpy.class.getMethods()) {
            if (method.getName().contains(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException();
    }

    class TargetSpy {
        private boolean isMethod1Invoked = false;
        private boolean isMethod2Invoked = false;

        public String method1(String s1, String s2) {
            isMethod1Invoked = true;
            return s1 + s2;
        }

        public Object method2() {
            isMethod2Invoked = true;
            return new Object();
        }

        public boolean isMethod1Invoked() {
            return isMethod1Invoked;
        }

        public boolean isMethod2Invoked() {
            return isMethod2Invoked;
        }
    }
}