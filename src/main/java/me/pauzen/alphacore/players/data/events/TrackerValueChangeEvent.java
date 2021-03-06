/*
 *  Created by Filip P. on 2/15/15 12:13 PM.
 */

package me.pauzen.alphacore.players.data.events;

import me.pauzen.alphacore.events.CallableEvent;
import me.pauzen.alphacore.players.data.trackers.Tracker;
import org.bukkit.event.HandlerList;

public class TrackerValueChangeEvent extends CallableEvent {

    private static final HandlerList handlers = new HandlerList();
    private int     oldValue;
    private int     newValue;
    private Tracker tracker;

    public TrackerValueChangeEvent(int oldValue, int newValue, Tracker tracker) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.tracker = tracker;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public int getOldValue() {
        return oldValue;
    }

    public int getNewValue() {
        return newValue;
    }

    public Tracker getTracker() {
        return tracker;
    }
}
