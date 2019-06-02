package com.ansill.redis;

import javax.annotation.Nonnull;

/** Subscription reference that allows users to unsubscribe later on */
@SuppressWarnings("unused")
public class Subscription implements AutoCloseable{

    /** Runnable that closes the subscription */
    @Nonnull
    private final Runnable closing_runnable;

    /** Cancellation flag */
    private boolean is_canceled = false;

    /**
     * Subscription constructor
     *
     * @param closing_runnable runner that runs at closing event
     */
    Subscription(@Nonnull Runnable closing_runnable){
        this.closing_runnable = closing_runnable;
    }

    /** Cancels the subscription */
    @SuppressWarnings("WeakerAccess")
    public void cancel(){
        if(!is_canceled){
            this.is_canceled = true;
            this.closing_runnable.run();
        }
    }

    @Override
    public void close(){
        this.cancel();
    }

    /**
     * Checks if the subscription is canceled or not
     *
     * @return true if canceled, false if it's not canceled
     */
    public boolean isCanceled(){
        return this.is_canceled;
    }
}
