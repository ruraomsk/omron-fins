/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import com.tibbo.aggregate.common.Log;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwMasterDevice extends Thread
{

    private ConcurrentHashMap<Integer, FwOneReg> data = null;
    private ConcurrentLinkedQueue<FwOneReg> history = null;
    private ConcurrentLinkedQueue<FwBaseMess> inmessages = null;
    private ConcurrentLinkedQueue<FwBaseMess> outmessages = null;
    private Socket socket;
    private int controller;
    public int nomerinfo = 1;
    private FwInfo info = null;
    private FwRegisters tableDecode = null;
    private long stepTime = FwUtil.FP_STEP_TIME;
    public Date lastsync = new Date();
    private Date lastlive = new Date();
    private boolean connect = false;
    public FwTransport mytransport = null;
    private int countlive = 0;
    private long stepGiveMe = FwUtil.FP_STEP_GIVEME;
    private long stepLive = FwUtil.FP_STEP_LIVE;
    private byte errFuncCode = 0;
    private long lastGiveMe = new Date().getTime();
    private long lastLive = new Date().getTime();
    private String UPCMessage = "";

    public FwMasterDevice(Socket socket, int controller, FwRegisters tableDecode)
    {
        this.socket = socket;
        this.controller = controller;
        this.tableDecode = tableDecode;
        data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
        history = new ConcurrentLinkedQueue<>();
        inmessages = new ConcurrentLinkedQueue<>();
        outmessages = new ConcurrentLinkedQueue<>();
        Long now=System.currentTimeMillis();
        for (FwRegister reg : tableDecode.getCollection()) {
            if ((reg.getController() == controller)) {
                FwOneReg onereg = new FwOneReg(now, reg);
                data.put(reg.getKey(), onereg);
            }
        }
        for (FwOneReg oreg : data.values()) {
            history.add(oreg);
        }
    }

    public void gogo()
    {
        try {
            connect = false;
            mytransport = new FwTransport(socket, tableDecode);
            if (mytransport == null) {
                Log.CORE.error("Транспорт не запустился "+myAddress());
                return;
            }
            if (!(connect = mytransport.connect())) {
                Log.CORE.error("Транспорт нет связи "+myAddress());
                return;
            }
        }
        catch (Exception ex) {
                Log.CORE.error("Транспорт "+myAddress()+" "+ex.getMessage());
            return;
        }
        if (!connect) {
            return;
        }
        start();
    }

    public boolean isConnected()
    {
        return connect;
    }

    public void disconect()
    {
        mytransport.close();
        connect = false;
    }

    public String myAddress()
    {
        return socket.getInetAddress().getHostAddress() + ":" + Integer.toString(socket.getPort());
    }

    public String myIP()
    {
        return socket.getInetAddress().getHostAddress();
    }

    public int myPort()
    {
        return socket.getPort();
    }

    @Override
    public void run()
    {
        try {
//            FwSimul setSimul = new FwSimul(controller, FwUtil.FP_CODE_DISCR);
//            sendMessage(setSimul);
//            Thread.sleep(stepTime);

//            setSimul = new FwSimul(controller, FwUtil.FP_CODE_ANALOG);
//            sendMessage(setSimul);
//            Thread.sleep(stepTime);
            FwMesCtrl givemeAll = new FwMesCtrl(controller, FwUtil.FP_CTRL_ALLINFO);
            sendMessage(givemeAll);
            Thread.sleep(stepTime);

            FwMesCtrl setPeriod = new FwMesCtrl(controller, FwUtil.FP_CTRL_ALL);
            setPeriod.setPar(5, (int) (stepGiveMe / FwUtil.FP_CYCLE_CONTRL));
            sendMessage(setPeriod);
            Thread.sleep(stepTime);

            setPeriod = new FwMesCtrl(controller, FwUtil.FP_CTRL_CHPERIOD);
//            setPeriod.setPar(4, (int) (stepGiveMe / FwUtil.FP_CYCLE_CONTRL));
            setPeriod.setPar(5, (int) (stepGiveMe / FwUtil.FP_CYCLE_CONTRL));
            sendMessage(setPeriod);
            Thread.sleep(stepTime);
        }
        catch (InterruptedException ex) {
        }

        lastGiveMe = new Date().getTime();
        FwResponse resp;
        countlive = 1;
        try {
            while (!Thread.interrupted() && isConnected()) {
                long now = new Date().getTime();
                if ((now - lastGiveMe) > getStepGiveMe()) {
                    for (FwOneReg oreg : data.values()) {
                        history.add(oreg);
                    }
                    lastGiveMe = now;
                }
                if ((now - lastLive) > getStepLive()) {
                    FwMesLive meslive = new FwMesLive(controller, countlive++);
                    if (countlive > 32000) {
                        countlive = 1;
                    }
                    sendMessage(meslive);
                    lastLive = now;
                }
                while ((resp = mytransport.readMessage()) != null) {
                    if (resp.getController() != controller) {
                        Log.CORE.error("Неверный номер контроллера " + Integer.toString(resp.getController()));
                        errFuncCode = resp.getFunctionCode();
                        disconect();
                        break;
                    }
                    switch (resp.getFunctionCode()) {
                        case FwUtil.FP_CODE_INFO:
                            readValues(resp.getInfo());
                            break;
                        case FwUtil.FP_CODE_35H:
                            outmessages.add(resp.getKvit());
                            break;
                        case FwUtil.FP_CODE_36H:
                            outmessages.add(resp.getSetup());
                            break;
                        case FwUtil.FP_CODE_30H:
                            readDiags(resp.getDiag());
                            break;
                        case FwUtil.FP_CODE_91H:
                            outmessages.add(resp.getSynctime());
                            break;
                    }
                }
                FwBaseMess message;
                while ((message = inmessages.poll()) != null) {
                    sendMessage(message);
                }
                Thread.sleep(stepTime);
                connect = mytransport.isConnected();
            }

        }
        catch (InterruptedException ex) {
            Log.CORE.error("MasterDevice "+myAddress()+" "+ ex.getMessage());
            disconect();
        }
    }

    private void readValues(FwInfo info)
    {
        if(info==null){
            Log.CORE.error("Пустой инфо!");
            return;
        }
            
        nomerinfo = info.getNomer();
        for (int i = 0; i < info.getSize(); i++) {
            FwOneReg value = info.getOneReg(i);
            history.add(value);

            data.put(value.getReg().getKey(), value);
        }
    }

    public FwOneReg getOneReg(int uId)
    {
        int key = FwRegister.makeKey(controller, uId, true);

        return data.get(key);
    }

    public FwOneReg getHistory()
    {
        return history.poll();
    }

    public FwBaseMess readMessage()
    {
        return outmessages.poll();
    }

    public void putMessage(FwBaseMess message)
    {
        inmessages.add(message);
    }

    public FwRegisters getTableDecode()
    {
        return tableDecode;
    }

    public int getController()
    {
        return controller;
    }

    public long getStepTime()
    {
        return stepTime;
    }

    public void setStepTime(long newStep)
    {
        stepTime = newStep;
    }

    public void setStepGiveMe(long newStep)
    {
        stepGiveMe = newStep;
    }

    public void setStepLive(long newStep)
    {
        stepLive = newStep;
    }

    private void sendMessage(FwBaseMess message)
    {
        mytransport.writeMessage(new FwMessage(message));
    }

    public Object getValue(int uId)
    {
        int key = FwRegister.makeKey(controller, uId, true);
        if (data.get(key) == null) {
            return null;
        }
        return data.get(key).getValue();
    }

    public Object getCode(int uId)
    {
        int key = FwRegister.makeKey(controller, uId, false);
        if (data.get(key) == null) {
            return null;
        }
        return data.get(key).getValue();
    }

    private void readDiags(FwDiag diag)
    {
        long now=System.currentTimeMillis();
        for (int i = 0; i < diag.getSize(); i++) {
            FwRegister one = tableDecode.getRegisterDiag(controller, diag.getDiagUId(i));
            if (one == null) {
                Log.CORE.error("Нет диагностики controller=" + Integer.toString(controller) + " uId=" + Integer.toString(diag.getDiagUId(i)));
                continue;
            }
            FwOneReg value = new FwOneReg(now, one);
            value.setValue(diag.getDiagCode(i));
            value.setGood(FwUtil.FP_DATA_GOOD);
            history.add(value);
            data.put(value.getReg().getKey(), value);
            //System.out.println("===uId="+Integer.toString(value.getuId())+" value="+value.getValue().toString());
        }
        UPCMessage = diag.getUPCMessage();
    }

    /**
     * @return the stepGiveMe
     */
    public long getStepGiveMe()
    {
        return stepGiveMe;
    }

    /**
     * @return the stepLive
     */
    public long getStepLive()
    {
        return stepLive;
    }

    /**
     * @return the errFuncCode
     */
    public byte getErrFuncCode()
    {
        return errFuncCode;
    }

    public String getUPCMessage()
    {
        return UPCMessage;
    }
}
