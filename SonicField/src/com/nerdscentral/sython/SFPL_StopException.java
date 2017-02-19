/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

/**
 * Used to jump out of code blocks.
 * 
 * @author AlexTu
 */
public class SFPL_StopException extends SFPL_RuntimeException
{

    private static final long serialVersionUID = 1L;
    private int               count;

    private final Object      operand;

    /**
     * Jumps out of the current code block and forward the toForward operand.
     * 
     * @param toForward
     * @param x
     *            the number of blocks to break out of
     */
    public SFPL_StopException(final Object toForward, final int x)
    {
        super(Messages.getString(Messages.getString("SFPL_StopException.0"))); //$NON-NLS-1$
        this.operand = toForward;
        this.count = x;
    }

    /**
     * Gets the operand to forward from the block.
     * 
     * @return the operand to forward from the broken out of block
     */
    public Object getOperand()
    {
        return this.operand;
    }

    /**
     * True if this needs throwing again, else false.
     * 
     * @return if it needs to be re-thrown
     */
    public boolean rethrow()
    {
        if (this.count == 1)
        {
            return false;
        }
        if (this.count == -1)
        {
            return true;
        }
        this.count--;
        return true;
    }

}
