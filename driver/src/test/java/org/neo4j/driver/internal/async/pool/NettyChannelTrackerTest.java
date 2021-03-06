/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.async.pool;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import org.neo4j.driver.internal.BoltServerAddress;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.neo4j.driver.internal.async.ChannelAttributes.setServerAddress;
import static org.neo4j.driver.internal.logging.DevNullLogging.DEV_NULL_LOGGING;
import static org.neo4j.driver.internal.metrics.InternalAbstractMetrics.DEV_NULL_METRICS;

public class NettyChannelTrackerTest
{
    private final BoltServerAddress address = BoltServerAddress.LOCAL_DEFAULT;
    private final NettyChannelTracker tracker = new NettyChannelTracker( DEV_NULL_METRICS, DEV_NULL_LOGGING );

    @Test
    public void shouldIncrementInUseCountWhenChannelCreated()
    {
        Channel channel = newChannel();
        assertEquals( 0, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );

        tracker.channelCreated( channel, null );
        assertEquals( 1, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );
    }

    @Test
    public void shouldIncrementInUseCountWhenChannelAcquired()
    {
        Channel channel = newChannel();
        assertEquals( 0, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );

        tracker.channelCreated( channel, null );
        assertEquals( 1, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );

        tracker.channelReleased( channel );
        assertEquals( 0, tracker.inUseChannelCount( address ) );
        assertEquals( 1, tracker.idleChannelCount( address ) );

        tracker.channelAcquired( channel );
        assertEquals( 1, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );
    }

    @Test
    public void shouldIncrementInuseCountForAddress()
    {
        Channel channel1 = newChannel();
        Channel channel2 = newChannel();
        Channel channel3 = newChannel();

        assertEquals( 0, tracker.inUseChannelCount( address ) );
        tracker.channelCreated( channel1, null );
        assertEquals( 1, tracker.inUseChannelCount( address ) );
        tracker.channelCreated( channel2, null );
        assertEquals( 2, tracker.inUseChannelCount( address ) );
        tracker.channelCreated( channel3, null );
        assertEquals( 3, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );
    }

    @Test
    public void shouldDecrementCountForAddress()
    {
        Channel channel1 = newChannel();
        Channel channel2 = newChannel();
        Channel channel3 = newChannel();

        tracker.channelCreated( channel1, null );
        tracker.channelCreated( channel2, null );
        tracker.channelCreated( channel3, null );
        assertEquals( 3, tracker.inUseChannelCount( address ) );
        assertEquals( 0, tracker.idleChannelCount( address ) );

        tracker.channelReleased( channel1 );
        assertEquals( 2, tracker.inUseChannelCount( address ) );
        assertEquals( 1, tracker.idleChannelCount( address ) );
        tracker.channelReleased( channel2 );
        assertEquals( 1, tracker.inUseChannelCount( address ) );
        assertEquals( 2, tracker.idleChannelCount( address ) );
        tracker.channelReleased( channel3 );
        assertEquals( 0, tracker.inUseChannelCount( address ) );
        assertEquals( 3, tracker.idleChannelCount( address ) );
    }

    @Test
    public void shouldThrowWhenDecrementingForUnknownAddress()
    {
        Channel channel = newChannel();

        try
        {
            tracker.channelReleased( channel );
            fail( "Exception expected" );
        }
        catch ( Exception e )
        {
            assertThat( e, instanceOf( IllegalStateException.class ) );
        }
    }

    @Test
    public void shouldReturnZeroActiveCountForUnknownAddress()
    {
        assertEquals( 0, tracker.inUseChannelCount( address ) );
    }

    private Channel newChannel()
    {
        EmbeddedChannel channel = new EmbeddedChannel();
        setServerAddress( channel, address );
        return channel;
    }
}
