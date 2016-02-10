package ru.wobot.uri.impl;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValueConverter_Test {

    @Test
    public void test_successful_convert() {
        // given
        ValueConverter c = new ValueConverter(Integer.class);
        // when
        ConvertResult convert = c.convert("789");
        //then
        assertThat(convert.isConvertSuccess(), is(true));
        assertThat((Integer) convert.getResult(), equalTo(789));
    }

    @Test
    public void test_unsuccessful_convert() {
        // given
        ValueConverter c = new ValueConverter(Integer.class);
        // when
        ConvertResult convert = c.convert("abc");
        //then
        assertThat(convert.isConvertSuccess(), is(false));
    }

    @Test
    public void test_primitive_type_convert() {
        // given
        ValueConverter c = new ValueConverter(int.class);
        // when
        ConvertResult convert = c.convert("123");
        //then
        assertThat((int) convert.getResult(), equalTo(123));
    }

    @Test
    public void test_parseValueTemplate() {
        // given
        // when
        String name = ValueConverter.parseValueTemplate("prefix{abc}");
        //then
        assertThat(name, is(equalTo("abc")));
    }
}