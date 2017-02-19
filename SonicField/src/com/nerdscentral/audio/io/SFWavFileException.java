/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio.io;

/**
 * @author a1t
 * 
 */
public class SFWavFileException extends Exception
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public SFWavFileException()
    {
        super();
    }

    public SFWavFileException(String message)
    {
        super(message);
    }

    public SFWavFileException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SFWavFileException(Throwable cause)
    {
        super(cause);
    }
}
