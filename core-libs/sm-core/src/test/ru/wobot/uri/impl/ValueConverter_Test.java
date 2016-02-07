package ru.wobot.uri.impl;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValueConverter_Test {

    @Test
    public void test_successful_convert() throws Exception {
        // given
        ValueConverter c = new ValueConverter(Integer.class);
        // when
        final ValueConverter.ConvertResult convert = c.convert("789");
        //then
        assertThat(convert.isConvertSuccess(), is(true));
        assertThat((Integer) convert.getResult(), equalTo(789));
    }

    @Test
    public void test_unsuccessful_convert() throws Exception {
        // given
        ValueConverter c = new ValueConverter(Integer.class);
        // when
        final ValueConverter.ConvertResult convert = c.convert("abc");
        //then
        assertThat(convert.isConvertSuccess(), is(false));
    }

    @Test
    public void test_parseValueTemplate() throws Exception {
        // given
        // when
        String name = ValueConverter.parseValueTemplate("{abc}");
        //then
        assertThat(name, is(equalTo("abc")));
    }
}