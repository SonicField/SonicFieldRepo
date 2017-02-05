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

    @SuppressWarnings("nls")
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
                InputStream sis = Sython.class.getClassLoader().getResourceAsStream("com/nerdscentral/sython/sython.py");
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
                    InputStream pis = Sython.class.getClassLoader()
                                    .getResourceAsStream("com/nerdscentral/sython/processors.txt");
                    InputStreamReader pir = new InputStreamReader(pis);
                    BufferedReader bpir = new BufferedReader(pir);)
                {
                    String lin = null;
                    HashMap<String, SFPL_Operator> processors = new HashMap<>();
                    interp.exec("import __builtin__");
                    while ((lin = bpir.readLine()) != null)
                    {
                        if (lin.trim().length() > 0)
                        {
                            SFPL_Operator op = (SFPL_Operator) Class.forName(lin).newInstance();
                            String word = op.Word();
                            processors.put(word, op);
                            init("    def " + word + "(self, input, *args):");
                            init("        return self.run(\"" + word + "\",input,args)");
                            init("    __builtin__." + word + "=" + word);
                        }
                    }
                    List<SFPL_Operator> vols = new ArrayList<>(404);
                    vols.addAll(SFP_DBs.getAll());
                    vols.addAll(SFP_Pcnt.getAll());
                    for (SFPL_Operator op : vols)
                    {
                        String word = op.Word();
                        processors.put(word, op);
                        init("    def " + word + "(self, input):");
                        init("        return self.run(\"" + word + "\",input,[])");
                        init("    __builtin__." + word + "=" + word);
                    }
                    init("");
                    System.out.println("Python about to interpret:");
                    // System.out.println(initCode);
                    interp.exec(initCode.toString());
                    PyObject pyo = interp.get("SonicField");
                    PyDictionary pid = new PyDictionary();
                    pid.putAll(processors);
                    PyObject sf = pyo.__call__(pid);
                    interp.exec("print \"Installing sf object\"");
                    interp.set("sf", sf);
                    interp.exec("__builtin__.sf=sf");
                }

                interp.exec("import __builtin__");
                interp.exec("__builtin__.sf=sf");
                interp.exec("import sython.concurrent");
                interp.exec("print \"Switching To Python Mode\"");
                interp.exec("print \"========================\"");
                long t0 = System.currentTimeMillis();
                for (String f : args)
                {
                    interp.exec(f);
                }
                interp.exec("sython.concurrent.sf_shutdown()");
                interp.exec("print \"========================\"");
                interp.exec("print \"------- All DONE -------\"");
                long t1 = System.currentTimeMillis();
                System.out.println("Total Processing Took: " + ((t1 - t0) / 1000) + " seconds");
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
                        System.err.println("DEADLOCK");
                        System.err.println("========");
                        ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                        for (ThreadInfo info : infos)
                        {
                            System.err.println("STACK:");
                            StackTraceElement[] stack = info.getStackTrace();
                            for (StackTraceElement x : stack)
                            {
                                System.err.println("    " + x);
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
