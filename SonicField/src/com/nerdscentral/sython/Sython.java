package com.nerdscentral.sython;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.utilities.SFP_DBs;
import com.nerdscentral.audio.utilities.SFP_Pcnt;

public class Sython
{
    private static StringBuilder initCode = new StringBuilder();

    private static void init(String what)
    {
        initCode.append(what);
        initCode.append(System.lineSeparator());
    }

    public static void main(String[] args)
    {
        try
        {
            // Launch Thread Watchdog.
            // Checks for deadlocks IFF the property SFConstants.FIND_DEADLOCKS is set.
            if (SFConstants.FIND_DEADLOCKS)
            {
                findDeadlocks();
            }

            // Create Java/Jython interface
            // ============================

            try (
                InputStream sis = Sython.class.getClassLoader().getResourceAsStream(Messages.getString("Sython.0")); //$NON-NLS-1$
                InputStreamReader sir = new InputStreamReader(sis);
                BufferedReader bsir = new BufferedReader(sir);)
            {
                {
                    String lin = null;
                    while ((lin = bsir.readLine()) != null)
                    {
                        if (lin.trim().length() > 0)
                        {
                            init(lin);
                        }
                    }
                }
            }

            // Load operators
            // ==============
            try (PythonInterpreter interp = new PythonInterpreter())
            {
                try (
                    InputStream pis = Sython.class.getClassLoader().getResourceAsStream(Messages.getString("Sython.1")); //$NON-NLS-1$
                    InputStreamReader pir = new InputStreamReader(pis);
                    BufferedReader bpir = new BufferedReader(pir);)
                {
                    String lin = null;
                    HashMap<String, SFPL_Operator> processors = new HashMap<>();
                    interp.exec(Messages.getString("Sython.2")); //$NON-NLS-1$
                    while ((lin = bpir.readLine()) != null)
                    {
                        if (lin.trim().length() > 0)
                        {
                            SFPL_Operator op = (SFPL_Operator) Class.forName(lin).newInstance();
                            String word = op.Word();
                            processors.put(word, op);
                            init(Messages.getString("Sython.3") + word + Messages.getString("Sython.4")); //$NON-NLS-1$ //$NON-NLS-2$
                            init(Messages.getString("Sython.5") + word + Messages.getString("Sython.6")); //$NON-NLS-1$ //$NON-NLS-2$
                            init(Messages.getString("Sython.7") + word + Messages.getString("Sython.8") + word); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                    List<SFPL_Operator> vols = new ArrayList<>(404);
                    vols.addAll(SFP_DBs.getAll());
                    vols.addAll(SFP_Pcnt.getAll());
                    for (SFPL_Operator op : vols)
                    {
                        String word = op.Word();
                        processors.put(word, op);
                        init(Messages.getString("Sython.9") + word + Messages.getString("Sython.10")); //$NON-NLS-1$ //$NON-NLS-2$
                        init(Messages.getString("Sython.11") + word + Messages.getString("Sython.12")); //$NON-NLS-1$ //$NON-NLS-2$
                        init(Messages.getString("Sython.13") + word + Messages.getString("Sython.14") + word); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    init(Messages.getString("Sython.15")); //$NON-NLS-1$
                    System.out.println(Messages.getString("Sython.16")); //$NON-NLS-1$
                    // System.out.println(initCode);
                    interp.exec(initCode.toString());
                    PyObject pyo = interp.get(Messages.getString("Sython.17")); //$NON-NLS-1$
                    PyDictionary pid = new PyDictionary();
                    pid.putAll(processors);
                    PyObject sf = pyo.__call__(pid);
                    interp.exec(Messages.getString("Sython.18")); //$NON-NLS-1$
                    interp.set(Messages.getString("Sython.19"), sf); //$NON-NLS-1$
                    interp.exec(Messages.getString("Sython.20")); //$NON-NLS-1$
                }

                interp.exec(Messages.getString("Sython.21")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.22")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.23")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.24")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.25")); //$NON-NLS-1$
                long t0 = System.currentTimeMillis();
                for (String f : args)
                {
                    interp.exec(f);
                }
                interp.exec(Messages.getString("Sython.26")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.27")); //$NON-NLS-1$
                interp.exec(Messages.getString("Sython.28")); //$NON-NLS-1$
                long t1 = System.currentTimeMillis();
                System.out.println(Messages.getString("Sython.29") + ((t1 - t0) / 1000) + Messages.getString("Sython.30")); //$NON-NLS-1$ //$NON-NLS-2$
            }

        }
        catch (Throwable t)
        {
            while (t != null)
            {
                t.printStackTrace();
                t = t.getCause();
            }
            System.exit(1);
        }
        System.exit(0);
    }

    private static void findDeadlocks()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                while (true)
                {
                    long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked.
                    if (threadIds != null)
                    {
                        System.err.println(Messages.getString("Sython.31")); //$NON-NLS-1$
                        System.err.println(Messages.getString("Sython.32")); //$NON-NLS-1$
                        ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                        for (ThreadInfo info : infos)
                        {
                            System.err.println(Messages.getString("Sython.33")); //$NON-NLS-1$
                            StackTraceElement[] stack = info.getStackTrace();
                            for (StackTraceElement x : stack)
                            {
                                System.err.println(Messages.getString("Sython.34") + x); //$NON-NLS-1$
                            }
                        }
                        System.exit(1);
                    }
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        // nop
                    }
                }

            }
        }).start();
    }
}
