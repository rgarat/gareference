package com.google.android.apps.analytics;

class CustomVariableBuffer
{
  private CustomVariable[] customVariables = new CustomVariable[5];

  public boolean isIndexAvailable(int index)
  {
    throwOnInvalidIndex(index);
    return this.customVariables[(index - 1)] == null;
  }

  public void setCustomVariable(CustomVariable customVariable)
  {
    int i = customVariable.getIndex();
    throwOnInvalidIndex(i);
    this.customVariables[(customVariable.getIndex() - 1)] = customVariable;
  }

  public CustomVariable getCustomVariableAt(int index)
  {
    throwOnInvalidIndex(index);
    return this.customVariables[(index - 1)];
  }

  public CustomVariable[] getCustomVariableArray()
  {
    return (CustomVariable[])this.customVariables.clone();
  }

  private void throwOnInvalidIndex(int index)
  {
    if ((index < 1) || (index > 5))
      throw new IllegalArgumentException("Index must be between 1 and 5 inclusive.");
  }

  public boolean hasCustomVariables()
  {
    for (int i = 0; i < this.customVariables.length; i++)
      if (this.customVariables[i] != null)
        return true;
    return false;
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.CustomVariableBuffer
 * JD-Core Version:    0.6.0
 */