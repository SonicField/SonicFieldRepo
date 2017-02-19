/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

public class Logger
{
    private final long        start = System.currentTimeMillis();
    private final SFPL_Logger outLogger;
    private final SFPL_Logger errLogger;

    public Logger(SFPL_Logger outLoggerIn, SFPL_Logger errLoggerIn)
    {
        outLogger = outLoggerIn;
        errLogger = errLoggerIn;
    }

    public void Log(String what)
    {
        // StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        outLogger.println(Messages.getString("Logger.0") + (System.currentTimeMillis() - start) + Messages.getString("Logger.1") + Thread.currentThread().getId()  //$NON-NLS-1$//$NON-NLS-2$ 
                        + /* trace[2].getClassName() + */Messages.getString("Logger.3") + what); //$NON-NLS-1$
    }

    public void printErrorln(String text)
    {
        errLogger.println(text);
    }
}
