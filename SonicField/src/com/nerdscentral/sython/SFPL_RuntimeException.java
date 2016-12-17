/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown by SFPL operators to indicate a runtime exception.
 * 
 * @author AlexTu
 * 
 */
public class SFPL_RuntimeException extends Exception
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               line_;
    private String            file_;
    private String            kword_;
    private int               colm_;

    /**
     * The line number that was parsed to create the operator which threw this exception. Note that of optimised code this might
     * not represent a perfect fit to the place in the original source, bit it should be close.
     * 
     * @return the line number
     */
    public int line()
    {
        return this.line_;
    }

    /**
     * The column number that was parsed to create the operator which threw this exception. Note that of optimised code this
     * might not represent a perfect fit to the place in the original source, bit it should be close.
     * 
     * @return the line number
     */
    public int column()
    {
        return this.colm_;
    }

    /**
     * The file (or other resource) which was parsed to create the operator which threw this exception.
     * 
     * @return the file (or other resource) name.
     */
    public String file()
    {
        return this.file_;
    }

    /**
     * The keyword for the operator which threw this exception. Note that for optimised code, this keyword might not be in the
     * original SFPL source.
     * 
     * @return the keyword
     */
    public String kword()
    {
        return this.kword_;
    }

    /**
     * Returns implementation the execution stack. This is useful for debugging the sfpl implementation. It does not drop out
     * the SFPL stack its self (if such a concept makes sense).
     * 
     * @return the stack as strings.
     */
    public List<String> GetSFPLStack()
    {
        final List<String> ret = new ArrayList<>();
        Throwable ex;
        ex = this;
        while (!(ex == null))
        {
            ret.add(ex.getMessage());
            ex = ex.getCause();
        }
        return ret;
    }

    /**
     * Creates a runtime exception to represent a failure in the SFPL running code.
     * 
     * @param t
     */
    public SFPL_RuntimeException(final Throwable t)
    {
        super(t);
    }

    /**
     * Creates a runtime exception to represent a failure in the SFPL running code.
     * 
     * @param message
     * @param cause
     */
    public SFPL_RuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public SFPL_RuntimeException(Exception e)
    {
        super(e);
    }

    public SFPL_RuntimeException(String message)
    {
        super(message);
    }

}
