/**
 This file is part of CallRec.

 CallRec is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CallRec is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CallRec.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.artem.callrec;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.Date;

/**
 * Created by 805268 on 04.05.2015.
 */
public abstract class PhoneCallReceiver extends BroadcastReceiver {

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
    static PhonecallStartEndDetector listener;
    String outgoingSavedNumber;
    protected Context savedContext;


    @Override
    public void onReceive(Context context, Intent intent) {
        savedContext = context;
        if(listener == null){
            listener = new PhonecallStartEndDetector();
        }

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            listener.setOutgoingNumber(intent.getExtras().getString("android.intent.extra.PHONE_NUMBER"));
            return;
        }

        //The other intent tells us the phone state changed.  Here we set a listener to deal with it
        TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    //Derived classes should override these to respond to specific events of interest

    protected abstract void onCallStarted(boolean isIncoming, String number, Date start);
     protected abstract void onCallEnded(boolean isIncoming, String number, Date start, Date end);

    //protected abstract void onIncomingCallStarted(String number, Date start);
   // protected abstract void onOutgoingCallStarted(String number, Date start);
  //  protected abstract void onIncomingCallEnded(String number, Date start, Date end);
 //   protected abstract void onOutgoingCallEnded(String number, Date start, Date end);
//    protected abstract void onMissedCall(String number, Date start);

    //Deals with actual events
    public class PhonecallStartEndDetector extends PhoneStateListener {
        int lastState = TelephonyManager.CALL_STATE_IDLE;
        Date callStartTime;
        boolean isIncoming;
        String savedNumber;  //because the passed incoming is only valid in ringing

        public PhonecallStartEndDetector() {}

        //The outgoing number is only sent via a separate intent, so we need to store it out of band
        public void setOutgoingNumber(String number){
            savedNumber = number;
        }

        //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
        //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if(lastState == state){
                //No change, debounce extras
                return;
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    isIncoming = true;
                    callStartTime = new Date();
                    savedNumber = incomingNumber;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //Transition of ringing->offhook are pickups of incoming calls.
                    if(lastState != TelephonyManager.CALL_STATE_RINGING){
                        isIncoming = false;
                        callStartTime = new Date();
                        onCallStarted(isIncoming, savedNumber, callStartTime);
                    }
                    else {
                        callStartTime = new Date();
                        onCallStarted(isIncoming, savedNumber, callStartTime);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                    if(lastState == TelephonyManager.CALL_STATE_RINGING){
                        //Ring but no pickup-  a miss
                    }
                    else {
                        onCallEnded(isIncoming, savedNumber, callStartTime, new Date());
                    }
                    break;
            }
            lastState = state;
        }

    }



}
