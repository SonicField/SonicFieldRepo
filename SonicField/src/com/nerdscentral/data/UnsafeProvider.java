package com.nerdscentral.data;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeProvider
{

    private static Unsafe getUnsafe()
    {
        try
        {
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); //$NON-NLS-1$
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static final Unsafe unsafe = getUnsafe();

}