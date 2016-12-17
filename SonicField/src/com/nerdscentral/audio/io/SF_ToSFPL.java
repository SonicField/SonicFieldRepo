package com.nerdscentral.audio.io;

import java.util.List;




import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;


public class SF_ToSFPL implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ToSFPL.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        StringBuilder buff = new StringBuilder();
        write(input, buff);
        return buff.toString();
    }

    static void write(Object what, StringBuilder buff) throws SFPL_RuntimeException
    {
        if (what instanceof String || what instanceof Number)
        {
            buff.append("\"" + what + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (what instanceof List)
        {
            List<Object> l = Caster.makeBunch(what);
            buff.append("("); //$NON-NLS-1$
            boolean comma = false;
            for (Object wInner : l)
            {
                write(wInner, buff);
                if (comma)
                {
                    buff.append(","); //$NON-NLS-1$
                }
                else
                {
                    comma = true;
                }
            }
            buff.append(")" + System.lineSeparator()); //$NON-NLS-1$
        }
        else
        {
            buff.append("[" + what.toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
