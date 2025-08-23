package com.android.internal.telephony;

interface ITelephony {
    long getAllowedNetworkTypesForReason(int subId, int reason);
    boolean setAllowedNetworkTypesForReason(int subId, int reason, long allowedNetworkTypes);
    int getPreferredNetworkType(int subId);
    boolean setPreferredNetworkType(int subId, int networkType);
}
