package com.google.android.apps.analytics;

abstract interface Dispatcher
{
  public abstract void dispatchEvents(Event[] events);

  public abstract void init(Callbacks callbacks, String paramString);

  public abstract void stop();

  public abstract void setDryRun(boolean dryRun);

  public abstract boolean isDryRun();

  public static abstract interface Callbacks
  {
    public abstract void eventDispatched(long paramLong);

    public abstract void dispatchFinished();
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.Dispatcher
 * JD-Core Version:    0.6.0
 */