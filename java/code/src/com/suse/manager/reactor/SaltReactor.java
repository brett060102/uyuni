/**
 * Copyright (c) 2015 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.suse.manager.reactor;

import com.redhat.rhn.common.messaging.MessageQueue;

import com.suse.manager.webui.models.MinionsModel;
import com.suse.saltstack.netapi.datatypes.Event;
import com.suse.saltstack.netapi.event.EventListener;
import com.suse.saltstack.netapi.event.EventStream;

import org.apache.log4j.Logger;

/**
 * Salt event reactor.
 */
public class SaltReactor implements EventListener {

    // Logger for this class
    private static Logger logger = Logger.getLogger(SaltReactor.class);

    // The event stream object
    private EventStream eventStream;

    // Indicate that the reactor has been stopped
    private volatile boolean isStopped = false;

    /**
     * Start the salt reactor.
     */
    public void start() {
        // Sync minions to systems in the database
        logger.debug("Syncing minions to the database");
        MinionsModel.getKeys().getMinions().forEach(this::triggerMinionRegistration);

        // Initialize the event stream
        eventStream = MinionsModel.getEventStream();
        eventStream.addEventListener(this);
    }

    /**
     * Stop the salt reactor.
     */
    public void stop() {
        isStopped = true;
        if (eventStream != null) {
            eventStream.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notify(Event event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Event: " + event.getTag() + " -> " + event.getData());
        }

        // Trigger minion registration on "salt/minion/*/start" events
        if (event.getTag().matches("salt/minion/(.*)/start")) {
            triggerMinionRegistration((String) event.getData().get("id"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eventStreamClosed() {
        logger.warn("Event stream has closed!");

        // Try to reconnect
        if (!isStopped) {
            logger.warn("Reconnecting to event stream...");
            eventStream = MinionsModel.getEventStream();
            eventStream.addEventListener(this);
        }
    }

    /**
     * Trigger the registration of a minion in case it is not registered yet.
     *
     * @param minionId the minion id of the minion to be registered
     */
    private void triggerMinionRegistration(String minionId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Trigger registration for minion: " + minionId);
        }
        MessageQueue.publish(new RegisterMinionEvent(minionId));
    }
}
