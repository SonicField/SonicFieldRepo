/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFInLineIIRFilter;

@SuppressWarnings("rawtypes")
public class Caster
{

    private final static Object[]               typeDescriptors;
    private final static HashMap<Class, String> typeDescriptionLookup;
    static
    {
        typeDescriptors = new Object[] { //
                Messages.getString("Caster.0"), SFSignal.class, //$NON-NLS-1$
                Messages.getString("Caster.1"), double.class,//$NON-NLS-1$
                Messages.getString("Caster.4"), boolean.class, //$NON-NLS-1$
                Messages.getString("Caster.5"), String.class, //$NON-NLS-1$
                Messages.getString("Caster.7"), SFInLineIIRFilter.class, //$NON-NLS-1$
                Messages.getString("Caster.9"), Sequence.class, //$NON-NLS-1$
                Messages.getString("Caster.11"), Sequencer.class,  //$NON-NLS-1$
                Messages.getString("Caster.10"), Track.class }; //$NON-NLS-1$
        typeDescriptionLookup = new HashMap<>();
        for (int i = 0; i < typeDescriptors.length; i += 2)
        {
            typeDescriptionLookup.put((Class) typeDescriptors[i + 1], (String) typeDescriptors[i]);
        }
    }

    public static Object checkAutoTranslation(Object o) throws SFPL_RuntimeException
    {

        if (o instanceof Future)
        {
            Future doer = (Future) o;
            try
            {
                Object d = doer.get();
                return checkAutoTranslation(d);
            }
            catch (Throwable t)
            {
                throw new SFPL_RuntimeException(t);
            }
        }

        if (o == null)
        {
            throw new SFPL_RuntimeException(Messages.getString("Caster.12"));             //$NON-NLS-1$
        }

        return o;
    }

    public final static double makeDouble(Object o) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(o);
        try
        {
            return ((Number) o2).doubleValue();
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Double.class, o2));
        }
    }

    public final static int makeSize(Object o) throws SFPL_RuntimeException
    {
        final double duration = (Caster.makeDouble(o)) / 1000.0d;
        return (int) (duration * SFConstants.SAMPLE_RATE);
    }

    private static String throwConversion(Class c, Object o) throws SFPL_RuntimeException
    {
        return String.format(Messages.getString("Caster.2"), c, describeSFPLType(o)); //$NON-NLS-1$
    }

    private static String describeSFPLType(Object o) throws SFPL_RuntimeException
    {
        String v = typeDescriptionLookup.get(o.getClass());
        if (v == null) throw new SFPL_RuntimeException(Messages.getString("Caster.3") + o.getClass()); //$NON-NLS-1$
        return v;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> makeBunch(Object input) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(input);
        try
        {
            return (List<Object>) o2;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(List.class, o2));
        }
    }

    public static boolean makeBoolean(Object o) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(o);
        try
        {
            return ((Boolean) o2).booleanValue();
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Boolean.class, o2));
        }
    }

    public static String makeString(Object input) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(input);
        try
        {
            return (String) o2;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(String.class, o2));
        }
    }

    public static SFSignal makeSFSignal(Object object) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(object);
        try
        {
            return (SFSignal) o2;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(SFSignal.class, o2));
        }
    }

    public static int makeInt(Object object) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(object);
        try
        {
            return ((Number) o2).intValue();
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Number.class, o2));
        }
    }

    public static long makeLong(Object input) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(input);
        try
        {
            return ((Number) o2).longValue();
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Double.class, o2));
        }
    }

    public static SFInLineIIRFilter makeFilter(Object input) throws SFPL_RuntimeException
    {
        Object o2 = checkAutoTranslation(input);
        try
        {
            return (SFInLineIIRFilter) o2;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(SFInLineIIRFilter.class, o2));
        }
    }

    public static Sequence makeMidiSequence(Object input) throws SFPL_RuntimeException
    {
        try
        {
            return (Sequence) input;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Sequence.class, input));
        }

    }

    public static Track makeMidiTrack(Object input) throws SFPL_RuntimeException
    {
        try
        {
            return (Track) input;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Track.class, input));
        }
    }

    public static Sequencer makeMidiSequencer(Object input) throws SFPL_RuntimeException
    {
        try
        {
            return (Sequencer) input;
        }
        catch (ClassCastException e)
        {
            throw new SFPL_RuntimeException(throwConversion(Sequencer.class, input));
        }
    }

    public static SFSignal incrReference(SFSignal ret)
    {
        return ret;
    }

}
