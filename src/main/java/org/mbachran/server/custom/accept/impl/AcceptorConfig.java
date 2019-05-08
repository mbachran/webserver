package org.mbachran.server.custom.accept.impl;

/**
 * Java Bean providing the Acceptor related application properties after they have been injected via the
 * value annotations at one place only.
 */
class AcceptorConfig
{
    static final String CONNECTION_POOL_QUALIFIER = "connectionPool";

    private final int minConnectionCount;

    private final int maxConnectionCount;

    private final int keepAliveTime;

    AcceptorConfig(int minConnectionCount, int maxConnectionCount, int keepAliveTime)
    {
        this.minConnectionCount = minConnectionCount;
        this.maxConnectionCount = maxConnectionCount;
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * @return The minimum number of threads to keep in the pool for the http connection threads.
     */
    int getMinConnectionCount()
    {
        return minConnectionCount;
    }

    /**
     * @return The maximum number of threads in the pool for the http connection threads.
     */
    int getMaxConnectionCount()
    {
        return maxConnectionCount;
    }

    /**
     * @return The keep alive time for a thread in the thread pool before abandoning it.
     */
    int getKeepAliveTime()
    {
        return keepAliveTime;
    }
}
