/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio.sound;

import javax.sound.sampled.LineListener;

/**
 * @author a1t
 * 
 */
public interface SFLineListener extends LineListener
{
    public boolean hasStopped();
}
