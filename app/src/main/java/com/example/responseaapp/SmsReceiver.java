package com.example.responseaapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

    private static SmsListener smsListener;

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle data = intent.getExtras();

        Object[] pdus = (Object[]) data.get("pdus");

        for(int i=0;i<pdus.length;i++){
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);

            String sender = smsMessage.getDisplayOriginatingAddress();
            //Check the sender to filter messages which we require to read



            String messageBody = smsMessage.getMessageBody()+"|"+sender;

            //Pass the message text to interface
            smsListener.messageReceived(messageBody);


        }

    }

    public static void bindListener(SmsListener listener){
        smsListener = listener;
    }
}
