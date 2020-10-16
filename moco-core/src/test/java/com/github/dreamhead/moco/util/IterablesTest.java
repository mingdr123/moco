package com.github.dreamhead.moco.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class IterablesTest {
    @Test
    public void should_get_tail() {
        assertThat(Iterables.tail(new Integer[]{1, 2}), is(new Integer[] {2}));
        assertThat(Iterables.tail(new Integer[1]), is(new Integer[0]));
    }
}