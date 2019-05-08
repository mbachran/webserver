package org.mbachran.server.custom.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to keep a runnable running within its thread until
 * - the delegate indicates that it is done
 * - the thread is interrupted like e.g. when an {@link java.util.concurrent.ExecutorService} is shutting down
 *
 * The delegate that can be passed to this wrapper on construction is actually a {@link Callable} that has to return an long value.
 * The contract based on the return value of the callable is as follows:
 * - negative => stop running
 * - zero => keep running and use the default delay that was given on construction
 * - positive => keep running and use the return value as delay before the next invocation only (reverting to the default subsequently)
 *
 * Exceptions or Errors thrown from the delegate (unless it is a {@link InterruptedException}) will not lead to the runnable stopping.
 * Trying to survive Errors can of course be debated especially there might be a risk of a busy loop depending on how the delegate is implemented.
 *
 * NOTE that thrown Exception or Errors will not lead to the delay being respected (maybe that is a bad idea?).
 *
 * Makes sure that all errors get logged.
 */
public class SurvivingRunnable implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(SurvivingRunnable.class);

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Callable<Long> delegate;

    private final long delay;

    /**
     * Defaults to a default delay of zero.
     *
     * @param delegate The Callable to help surviving.
     */
    public SurvivingRunnable(@Nonnull final Callable<Long> delegate)
    {
        this(delegate, 0);
    }

    /**
     * @param delegate The Callable to help surviving.
     * @param delay The default delay between invocations.
     */
    private SurvivingRunnable(@Nonnull final Callable<Long> delegate, final long delay)
    {
        this.delegate = delegate;
        this.delay = delay;
    }

    @Override
    public void run()
    {
        while (running.get())
        {
            try
            {
                // negative delay like -1 indicates to end the thread and let it return to the pool
                final long requestedDelay = delegate.call();
                if (requestedDelay < 0)
                {
                    LOG.info("Ending work as requested from delegate!");
                    stop();
                }
                else
                {
                    // zero means that the configured default delay will be used otherwise the positive delay will be picked up
                    final long nextDelay = requestedDelay == 0 ? delay : requestedDelay;
                    if (nextDelay > 0)
                    {
                        Thread.sleep(nextDelay);
                    }
                }
            }
            catch (InterruptedException e)
            {
                LOG.info("Ending work due to interruption!");
                stop();
            }
            catch (Throwable t)
            {
                LOG.warn("Faced an exception or error. Keeping runnable alive.", t);
            }
        }

        LOG.info("Stopped runnable.");
    }

    private void stop()
    {
        running.set(false);
    }
}
