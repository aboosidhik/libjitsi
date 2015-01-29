/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.audionotifier;

import java.util.concurrent.*;

/**
 * Represents an audio clip which could be played (optionally, in a loop) and
 * stopped..
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface SCAudioClip
{
    /**
     * Starts playing this audio once only. The method behaves as if
     * {@link #play(int, Callable)} was invoked with a negative
     * <tt>loopInterval</tt> and/or <tt>null</tt> <tt>loopCondition</tt>.
     */
    public void play();

    /**
     * Starts playing this audio. Optionally, the playback is looped.
     *
     * @param loopInterval the interval of time in milliseconds between
     * consecutive plays of this audio. If negative, this audio is played once
     * only and <tt>loopCondition</tt> is ignored.
     * @param loopCondition a <tt>Callable</tt> which is called at the beginning
     * of each iteration of looped playback of this audio except the first one
     * to determine whether to continue the loop. If <tt>loopInterval</tt> is
     * negative or <tt>loopCondition</tt> is <tt>null</tt>, this audio is played
     * once only.
     */
    public void play(int loopInterval, Callable<Boolean> loopCondition);

    /**
     * Stops playing this audio.
     */
    public void stop();

    /**
     * Determines whether this audio is started i.e. a <tt>play</tt> method was
     * invoked and no subsequent <tt>stop</tt> has been invoked yet.
     *
     * @return <tt>true</tt> if this audio is started; otherwise, <tt>false</tt>
     */
    public boolean isStarted();

    /**
     * Determines whether this audio file is valid.
     *
     * @return <tt>true</tt> if this audio file is in an acceptable format;
     * <tt>false</tt> otherwise.
     */
    public boolean isValid();

    /**
     * Registers the given listener with this SCAudioClip, so that we notify it
     * of changes in the state of the audio clip.
     *
     * @param listener The listener to register.
     */
    public void registerAudioListener(AudioListener listener);

    /**
     * Unregisters the given listener.
     *
     * @param listener The listener to unregister.
     */
    public void removeAudioListener(AudioListener listener);

    /**
     * Force the audio clip to play even if the AudioNotificationService is
     * muted. This is useful for changing the ringtone for example.
     */
	public void playWhenMuted();
}
