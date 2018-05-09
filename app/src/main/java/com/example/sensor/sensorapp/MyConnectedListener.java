package com.example.sensor.sensorapp;
import java.util.ArrayList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import zephyr.android.BioHarnessBT.*;


public class MyConnectedListener extends ConnectListenerImpl{
    private Handler _aNewHandler;
    private final int GP_MSG_ID = 0x20;
    private final int BREATHING_MSG_ID = 0x21;
    private final int ECG_MSG_ID = 0x22;                 // in use
    private final int RtoR_MSG_ID = 0x24;                // in use
    private final int ACCEL_100mg_MSG_ID = 0x2A;
    private final int SUMMARY_MSG_ID = 0x2B;

    private final int HEART_RATE = 0x100;        // in use
    private final int RESPIRATION_RATE = 0x101;  // in use
    private final int SKIN_TEMPERATURE = 0x102;
    private final int POSTURE = 0x103;
    private final int PEAK_ACCLERATION = 0x104;
    private final int R_to_R_PACKET = 0x105;     // in use
    private final int ECG_PACKET = 0x106;        // in use
    private final int NewRmssd_PACKET = 0x108;   // in use
    private final int BREATHING_WAVEFORM = 0x109; // in use
    private final int ZT = 0x110; // in use
    private final int SUMMARY_PACKET = 0x112;


    /*Creating the different Objects for different types of Packets*/
    private GeneralPacketInfo GPInfo = new GeneralPacketInfo();
    private ECGPacketInfo ECGInfoPacket = new ECGPacketInfo();
    private BreathingPacketInfo BreathingInfoPacket = new  BreathingPacketInfo();
    private RtoRPacketInfo RtoRInfoPacket = new RtoRPacketInfo();
    private AccelerometerPacketInfo AccInfoPacket = new AccelerometerPacketInfo();
    private SummaryPacketInfo SummaryInfoPacket = new SummaryPacketInfo();

    private PacketTypeRequest RqPacketType = new PacketTypeRequest();
    public MyConnectedListener(Handler _NewHandler) {
        super(_NewHandler, null);
        _aNewHandler = _NewHandler;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void Connected(ConnectedEvent<BTClient> eventArgs) {

        RqPacketType.GP_ENABLE = true;
        RqPacketType.BREATHING_ENABLE = true;
        RqPacketType.LOGGING_ENABLE = true;
        RqPacketType.ECG_ENABLE = true;
        RqPacketType.RtoR_ENABLE = true;
        RqPacketType.SUMMARY_ENABLE = true;




        ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms(), RqPacketType);
        _protocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
            int Exa = 65531 ;
            int sampleLast = 0;
            int windowWidth = 15;
            int stepLength = 3;
            fifo rmssdFIFO = new fifo(windowWidth/stepLength);
            fifo numSampFIFO = new fifo(windowWidth/stepLength);
            int stepCount = 0;
            int sumNew = 0;
            int numSamp = 0;

            @Override
            public void ReceivedPacket(ZephyrPacketEvent eventArgs) {
                ZephyrPacketArgs msg = eventArgs.getPacket();
                int MsgID = msg.getMsgID();
                byte [] DataArray = msg.getBytes();

                switch (MsgID)
                {

                    case GP_MSG_ID:

                        /* Obtain time from BioHarness */
                        int Zyear = GPInfo.GetTSYear(DataArray);
                        int Zmonth = GPInfo.GetTSMonth(DataArray);
                        int Zday = GPInfo.GetTSDay(DataArray);
                        long Zmin = GPInfo.GetMsofDay(DataArray);
                        long Zhour = Zmin/3600000;
                        long Zmins = (Zmin%3600000)/60000;
                        long Zsecs = ((Zmin%3600000)%60000)/1000;

                        String ZephyrTime = String.valueOf(Zyear) + "_" + String.valueOf(Zmonth) + "_" + String.valueOf(Zday) + " " + String.valueOf(Zhour) + ":" + String.valueOf(Zmins) + ":" + String.valueOf(Zsecs);

                        /* Heart Rate block */
                        int HRate =  GPInfo.GetHeartRate(DataArray);
                        Message text1 = _aNewHandler.obtainMessage(HEART_RATE);
                        Bundle b1 = new Bundle();
                        b1.putString("HeartRate", String.valueOf(HRate));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        Message ZephyrTMessage = _aNewHandler.obtainMessage(ZT);
                        Bundle ZTBundle = new Bundle();
                        ZTBundle.putString("ZephyrTime", ZephyrTime);
                        ZephyrTMessage.setData(ZTBundle);
                        _aNewHandler.sendMessage(ZephyrTMessage);

                        /* Respiration Rate block */
                        double RespRate = GPInfo.GetRespirationRate(DataArray);

                        text1 = _aNewHandler.obtainMessage(RESPIRATION_RATE);
                        b1.putString("RespirationRate", String.valueOf(RespRate));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        /* Skin Temperature block */
                        double SkinTempDbl = GPInfo.GetSkinTemperature(DataArray);
                        text1 = _aNewHandler.obtainMessage(SKIN_TEMPERATURE);

                        b1.putString("SkinTemperature", String.valueOf(SkinTempDbl));

                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        /* Posture block */

                        int PostureInt = GPInfo.GetPosture(DataArray);
                        text1 = _aNewHandler.obtainMessage(POSTURE);
                        b1.putString("Posture", String.valueOf(PostureInt));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        /* Peak Acceleration */

                        double PeakAccDbl = GPInfo.GetPeakAcceleration(DataArray);
                        text1 = _aNewHandler.obtainMessage(PEAK_ACCLERATION);
                        b1.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        byte ROGStatus = GPInfo.GetROGStatus(DataArray);

                        break;
                    /* breathing waveform */
                    case BREATHING_MSG_ID:
                        short BreathingSample[]; // = new short[17];
                        BreathingSample = BreathingInfoPacket.GetBreathingSamples(DataArray);

                        Message brWaveMessage = _aNewHandler.obtainMessage(BREATHING_WAVEFORM);
                        Bundle brWaveBundle = new Bundle();
                        brWaveBundle.putShortArray("brWaveform", BreathingSample);
                        brWaveMessage.setData(brWaveBundle);
                        _aNewHandler.sendMessage(brWaveMessage);

                        break;
                    /* R to R intervals */
                    case RtoR_MSG_ID:
                        Bundle bRMSSDm = new Bundle();
                        if(stepCount == stepLength - 1){
                            stepCount = 0;
                        }
                        else{
                            stepCount++;
                        }

                        int RtoRSamples[]; // = new int[18];
                        RtoRSamples = RtoRInfoPacket.GetRtoRSamples(msg.getBytes());

                        /*** filter out redundant samples***/
                        int [] realSamples = sampleFilter(RtoRSamples, Exa);
                        if(realSamples != null){
                            int L_index = realSamples.length;
                            Exa = realSamples[L_index-1];
                            int Temp;

                            Message text2; // = new Message();

                            Bundle b2 = new Bundle();
                            ArrayList<Integer> RRarrayList = new ArrayList<Integer>();
                            for(int p=0;p<L_index;p++){
                                /*** pass R to R interval to UI ***/
                                RRarrayList.add(realSamples[p]);

                                if(p>0){
                                    Temp = (int) Math.pow(realSamples[p] - realSamples[p-1], 2);
                                }
                                else {
                                    if(sampleLast != 0){
                                        Temp = (int) Math.pow(realSamples[p] - sampleLast, 2);
                                    }
                                    else{
                                        Temp = 0;
                                    }
                                }

                                if(p == L_index-1){
                                    sampleLast = realSamples[p];
                                }
                                sumNew = sumNew + Temp;
                            }
                            numSamp = numSamp + L_index;
                            text2 = _aNewHandler.obtainMessage(R_to_R_PACKET);
                            b2.putIntegerArrayList("RtoRText", RRarrayList);
                            text2.setData(b2);
                            _aNewHandler.sendMessage(text2);
                        }
                        else{
                            // System.out.println("Current packet has no valid R 2 R intervals !");
                        }

                        /*** compute window RMSSD ***/
                        if(stepCount == stepLength - 1) {
//	                    	numSamp = L_index;
                            // System.out.println("stepCount is stepLength");
                            rmssdFIFO.addElement(sumNew);
                            numSampFIFO.addElement(numSamp);
                            sumNew = 0;
                            numSamp = 0;

                            if(rmssdFIFO.isFull()){
                                // System.out.println("FIFO is full ! and # sample is "+numSampFIFO.returnSum());
                                double rmssdNew = Math.pow(rmssdFIFO.returnSum() / numSampFIFO.returnSum()-1, 0.5) ;
                                // System.out.println("rmssdNew is "+rmssdNew);
                                Message rmssdM = _aNewHandler.obtainMessage(NewRmssd_PACKET);
                                bRMSSDm.putString("NewRMSSD_Text", String.valueOf((int) rmssdNew));
                                rmssdM.setData(bRMSSDm);
                                _aNewHandler.sendMessage(rmssdM);
                                /*** send New Rmssd to front ***/
                                /* for Monitor status */
                            }
                        }
                        break;

                    /* ECG information */
                    case ECG_MSG_ID:
                        short[] ECGSampleArr; // = new short[63];
                        ECGSampleArr = ECGInfoPacket.GetECGSamples(DataArray);
                        Message text; // = new Message();
                        Bundle b3 = new Bundle();

                        text = _aNewHandler.obtainMessage(ECG_PACKET);
                        b3.putShortArray("ecgText", ECGSampleArr);
                        text.setData(b3);
                        _aNewHandler.sendMessage(text);
                        break;
                    case ACCEL_100mg_MSG_ID:

                        break;
                    case SUMMARY_MSG_ID:

                        double ActValue = SummaryInfoPacket.GetActivity(DataArray);
                        Message ActMessage; // = new Message();

                        ActMessage = _aNewHandler.obtainMessage(SUMMARY_PACKET);
                        Bundle ActBundle = new Bundle();
                        ActBundle.putString("Activity", String.valueOf(ActValue));
                        ActMessage.setData(ActBundle);
                        _aNewHandler.sendMessage(ActMessage);
                        break;
                }
            }

            private int[] sampleFilter(int[] RtoRSample, int Exa){

                int R_index = 1;
                int realIndex = 0;
                int R2RL = RtoRSample.length + 1;
                int[] toCmp = new int [R2RL];
                int[] realSamples = new int [R2RL-1];
                int[] nothing = null ;
                float thL = 0.75f;
                int thH = 1400;

//			System.out.println("sampleFilter method is called here !!!");

                for(int i=0;i<R2RL-1;i++){
                    realSamples[i] = 65530 ;
                }

                for(int i=0;i<R2RL;i++){
                    if(i==0){
                        toCmp[i] = Exa;
                    }
                    else{
                        toCmp[i] = RtoRSample[i-1];
                    }
                }

                while(R_index < R2RL){
//				if(toCmp[R_index-1] == 65531){
//					realSamples[realIndex] = toCmp[R_index] ;
//					realIndex++;
//					R_index++;
//				}
//				else if((toCmp[R_index] != toCmp[R_index-1]) && (toCmp[R_index] > thL*toCmp[R_index]) && (toCmp[R_index] < thH)){

                    if(toCmp[R_index] != toCmp[R_index-1]){
                        realSamples[realIndex] = toCmp[R_index] ;
                        realIndex++;
                        R_index++;
                    }
                    else R_index++;
                }


                int j=0;


                while(realSamples[j] != 65530){
                    j++;
                }
                if(j==0){
                    System.out.println("j is 0 and nothing will be returned !!!");
                    return nothing;
                }
                else {

                    int[] realSample = new int [j] ;

                    for(int i=0;i<j;i++){
                        realSample[i] = realSamples[i];
                    }
//				System.out.println("realSample will be returned !!!");
                    return realSample;
                }
            } /*** end of the added sampleFilter method ***/

        });
    }

}
